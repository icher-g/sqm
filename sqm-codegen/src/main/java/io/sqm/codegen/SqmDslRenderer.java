package io.sqm.codegen;

import io.sqm.core.DeleteStatement;
import io.sqm.core.InsertStatement;
import io.sqm.core.Lateral;
import io.sqm.core.MergeStatement;
import io.sqm.core.NamedParamExpr;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.Statement;
import io.sqm.core.Table;
import io.sqm.core.TableRef;
import io.sqm.core.UpdateStatement;
import io.sqm.core.WithQuery;
import io.sqm.core.walk.RecursiveNodeVisitor;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Renders generated Java source that reconstructs parsed SQM statements through the DSL helpers.
 */
public final class SqmDslRenderer {

    private static final String NEWLINE = "\n";
    private static final String INDENT = "    ";

    private final SqlFileCodegenOptions options;
    private final SqmJavaEmitter emitter;

    /**
     * Creates a renderer for generated DSL source.
     *
     * @param options code generation options
     */
    public SqmDslRenderer(SqlFileCodegenOptions options) {
        this.options = options;
        this.emitter = new SqmJavaEmitter();
    }

    private static String indentContinuationLines(String value, int spaces) {
        var indent = " ".repeat(Math.max(0, spaces));
        return value.replace(NEWLINE, NEWLINE + indent);
    }

    private static String renderSetLiteral(Collection<String> values) {
        if (values.isEmpty()) {
            return "Set.of()";
        }
        var ordered = new ArrayList<>(values);
        ordered.sort(String::compareTo);
        var joined = ordered.stream()
            .map(value -> "\"" + escapeJavaString(value) + "\"")
            .reduce((left, right) -> left + ", " + right)
            .orElse("");
        return "Set.of(" + joined + ")";
    }

    private static Set<String> collectNamedParameters(Statement statement) {
        var collector = new NamedParameterCollector();
        statement.accept(collector);
        return collector.parameters();
    }

    private static List<GeneratedStatementMethod> statementMethods(SqlSourceFile file, Set<String> usedClassMethodNames) {
        var methods = new ArrayList<GeneratedStatementMethod>(file.statements().size());
        for (int i = 0; i < file.statements().size(); i++) {
            var statement = file.statements().get(i);
            var methodName = uniqueMethodName(statementMethodBase(statement, i + 1), usedClassMethodNames);
            usedClassMethodNames.add(methodName + "Params");
            methods.add(new GeneratedStatementMethod(methodName, statement, collectNamedParameters(statement)));
        }
        return List.copyOf(methods);
    }

    private static String uniqueMethodName(String baseName, Set<String> used) {
        var candidate = baseName;
        var suffix = 2;
        while (used.contains(candidate) || used.contains(candidate + "Params")) {
            candidate = baseName + suffix;
            suffix++;
        }
        used.add(candidate);
        return candidate;
    }

    private static String statementMethodBase(Statement statement, int statementIndex) {
        var tableName = mainTableName(statement);
        if (tableName == null || tableName.isBlank()) {
            return "statement" + statementIndex;
        }
        var suffix = NameNormalizer.toUpperCamelName(tableName);
        if (statement instanceof SelectQuery || statement instanceof WithQuery) {
            return "get" + suffix;
        }
        return switch (statement) {
            case InsertStatement ignored -> "insert" + suffix;
            case UpdateStatement ignored -> "update" + suffix;
            case DeleteStatement ignored -> "delete" + suffix;
            case MergeStatement ignored -> "merge" + suffix;
            default -> "statement" + statementIndex;
        };
    }

    private static String mainTableName(Statement statement) {
        if (statement instanceof SelectQuery selectQuery) {
            return mainTableName(selectQuery.from());
        }
        if (statement instanceof WithQuery withQuery && withQuery.body() != null) {
            return mainTableName(withQuery.body());
        }
        if (statement instanceof InsertStatement insertStatement) {
            return tableName(insertStatement.table());
        }
        if (statement instanceof UpdateStatement updateStatement) {
            return tableName(updateStatement.table());
        }
        if (statement instanceof DeleteStatement deleteStatement) {
            return tableName(deleteStatement.table());
        }
        if (statement instanceof MergeStatement mergeStatement) {
            return tableName(mergeStatement.target());
        }
        return null;
    }

    private static String mainTableName(Query query) {
        if (query instanceof Statement statement) {
            return mainTableName(statement);
        }
        return null;
    }

    private static String mainTableName(TableRef tableRef) {
        if (tableRef instanceof Table table) {
            return tableName(table);
        }
        if (tableRef instanceof Lateral lateral) {
            return mainTableName(lateral.inner());
        }
        return null;
    }

    private static String tableName(Table table) {
        return table == null || table.name() == null ? null : table.name().value();
    }

    private static String normalizePath(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static String escapeJavaString(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Renders a Java source file for the given SQL folder group.
     *
     * @param group SQL folder group to render
     * @return generated Java source code
     */
    public String render(SqlFolderGroup group) {
        var code = new StringBuilder();
        code.append("package ").append(options.basePackage()).append(";").append(NEWLINE).append(NEWLINE);
        if (options.includeGenerationSourceAnnotations()) {
            code.append("import javax.annotation.processing.Generated;").append(NEWLINE);
        }
        code.append("import io.sqm.core.*;").append(NEWLINE);
        code.append("import java.util.Set;").append(NEWLINE);
        code.append("import java.util.List;").append(NEWLINE);
        code.append("import static io.sqm.dsl.Dsl.*;").append(NEWLINE).append(NEWLINE);
        if (options.includeGenerationSourceAnnotations()) {
            code.append("/**").append(NEWLINE);
            code.append(" * Generated from SQL files located in ").append(normalizePath(group.folder())).append(".").append(NEWLINE);
            code.append(" * Dialect: ").append(options.dialect().name()).append(".").append(NEWLINE);
            code.append(" * Source SQL paths:").append(NEWLINE);
            for (var file : group.files()) {
                code.append(" * - ").append(normalizePath(file.relativePath())).append(NEWLINE);
            }
            code.append(" */").append(NEWLINE);
            code.append(renderGeneratedAnnotation(group)).append(NEWLINE);
        }
        code.append("public final class ").append(group.className()).append(" {").append(NEWLINE).append(NEWLINE);
        code.append(INDENT).append("private ").append(group.className()).append("() {").append(NEWLINE);
        code.append(INDENT).append("}").append(NEWLINE).append(NEWLINE);
        var usedClassMethodNames = new HashSet<String>();
        for (var file : group.files()) {
            usedClassMethodNames.add(file.methodName());
            usedClassMethodNames.add(file.methodName() + "Params");
        }
        for (var file : group.files()) {
            var statementMethods = statementMethods(file, usedClassMethodNames);
            if (statementMethods.size() == 1) {
                renderStatementMethod(code, file, file.methodName(), file.statements().getFirst(), file.parameters(), false);
            }
            else {
                renderSequenceMethod(code, file, statementMethods);
                renderParamsMethod(code, file.methodName(), normalizePath(file.relativePath()), file.parameters());
                for (var method : statementMethods) {
                    renderStatementMethod(code, file, method.methodName(), method.statement(), method.parameters(), true);
                }
            }
        }
        code.append("}").append(NEWLINE);
        return code.toString();
    }

    private void renderStatementMethod(
        StringBuilder code,
        SqlSourceFile file,
        String methodName,
        Statement statement,
        Set<String> parameters,
        boolean perStatement
    ) {
        String statementExpression;
        try {
            statementExpression = emitter.emit(statement);
        } catch (IllegalStateException ex) {
            throw new SqlFileCodegenException(normalizePath(file.relativePath()) + ": " + ex.getMessage());
        }
        code.append(INDENT).append("/**").append(NEWLINE);
        if (options.includeGenerationSourceAnnotations()) {
            code.append(INDENT).append(" * SQL source: ").append(normalizePath(file.relativePath())).append(NEWLINE);
            code.append(INDENT).append(" *").append(NEWLINE);
        }
        else {
            code.append(INDENT).append(perStatement ? " * Gets a generated statement from a multi-statement SQL source." : " * Gets a generated statement.").append(NEWLINE);
            code.append(INDENT).append(" *").append(NEWLINE);
        }
        code.append(INDENT).append(" * @return statement model for this SQL source.").append(NEWLINE);
        code.append(INDENT).append(" */").append(NEWLINE);
        code.append(INDENT).append("public static ")
            .append(statement.getTopLevelInterface().getSimpleName()).append(" ")
            .append(methodName).append("() {").append(NEWLINE);
        code.append(INDENT).append(INDENT).append("return ")
            .append(indentContinuationLines(statementExpression, 12))
            .append(";").append(NEWLINE);
        code.append(INDENT).append("}").append(NEWLINE).append(NEWLINE);
        renderParamsMethod(code, methodName, normalizePath(file.relativePath()), parameters);
    }

    private void renderSequenceMethod(StringBuilder code, SqlSourceFile file, List<GeneratedStatementMethod> statementMethods) {
        code.append(INDENT).append("/**").append(NEWLINE);
        if (options.includeGenerationSourceAnnotations()) {
            code.append(INDENT).append(" * SQL source: ").append(normalizePath(file.relativePath())).append(NEWLINE);
            code.append(INDENT).append(" *").append(NEWLINE);
        }
        else {
            code.append(INDENT).append(" * Gets a generated statement sequence.").append(NEWLINE);
            code.append(INDENT).append(" *").append(NEWLINE);
        }
        code.append(INDENT).append(" * @return statement sequence model for this SQL source.").append(NEWLINE);
        code.append(INDENT).append(" */").append(NEWLINE);
        code.append(INDENT).append("public static StatementSequence ").append(file.methodName()).append("() {").append(NEWLINE);
        code.append(INDENT).append(INDENT).append("return statementSequence(");
        for (int i = 0; i < statementMethods.size(); i++) {
            if (i > 0) {
                code.append(", ");
            }
            code.append(statementMethods.get(i).methodName()).append("()");
        }
        code.append(");").append(NEWLINE);
        code.append(INDENT).append("}").append(NEWLINE).append(NEWLINE);
    }

    private void renderParamsMethod(StringBuilder code, String methodName, String relativePath, Set<String> parameters) {
        code.append(INDENT).append("/**").append(NEWLINE);
        if (options.includeGenerationSourceAnnotations()) {
            code.append(INDENT).append(" * Returns named parameters referenced by ").append(relativePath).append(".").append(NEWLINE);
        }
        else {
            code.append(INDENT).append(" * Returns named parameters.").append(NEWLINE);
        }
        code.append(INDENT).append(" *").append(NEWLINE);
        code.append(INDENT).append(" * @return immutable set of named parameter identifiers.").append(NEWLINE);
        code.append(INDENT).append(" */").append(NEWLINE);
        code.append(INDENT).append("public static Set<String> ").append(methodName).append("Params() {").append(NEWLINE);
        code.append(INDENT).append(INDENT).append("return ").append(renderSetLiteral(parameters)).append(";").append(NEWLINE);
        code.append(INDENT).append("}").append(NEWLINE).append(NEWLINE);
    }

    private String renderGeneratedAnnotation(SqlFolderGroup group) {
        var comments = new StringBuilder("dialect=")
            .append(options.dialect().name())
            .append("; sqlFolder=")
            .append(normalizePath(group.folder()))
            .append("; sqlFiles=");
        for (int i = 0; i < group.files().size(); i++) {
            if (i > 0) {
                comments.append(",");
            }
            comments.append(normalizePath(group.files().get(i).relativePath()));
        }

        var annotation = new StringBuilder();
        annotation.append("@Generated(").append(NEWLINE);
        annotation.append(INDENT).append("value = \"io.sqm.codegen.SqlFileCodeGenerator\",").append(NEWLINE);
        annotation.append(INDENT).append("comments = \"").append(escapeJavaString(comments.toString())).append("\"");
        if (options.includeGenerationTimestamp()) {
            annotation.append(",").append(NEWLINE);
            annotation.append(INDENT).append("date = \"").append(Instant.now().toString()).append("\"").append(NEWLINE);
            annotation.append(")");
            return annotation.toString();
        }
        annotation.append(NEWLINE).append(")");
        return annotation.toString();
    }

    private static final class NamedParameterCollector extends RecursiveNodeVisitor<Void> {
        private final Set<String> parameters = new LinkedHashSet<>();

        Set<String> parameters() {
            return Set.copyOf(parameters);
        }

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitNamedParamExpr(NamedParamExpr p) {
            parameters.add(p.name());
            return defaultResult();
        }
    }
}
