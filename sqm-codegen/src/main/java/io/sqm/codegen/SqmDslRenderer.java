package io.sqm.codegen;

import io.sqm.core.SelectQuery;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;

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
        code.append("import javax.annotation.processing.Generated;").append(NEWLINE);
        code.append("import io.sqm.core.*;").append(NEWLINE);
        code.append("import java.util.Set;").append(NEWLINE);
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
        for (var file : group.files()) {
            String statementExpression;
            try {
                statementExpression = emitter.emitStatement(file.statement());
            } catch (IllegalStateException ex) {
                throw new SqlFileCodegenException(normalizePath(file.relativePath()) + ": " + ex.getMessage());
            }
            code.append(INDENT).append("/**").append(NEWLINE);
            if (options.includeGenerationSourceAnnotations()) {
                code.append(INDENT).append(" * SQL source: ").append(normalizePath(file.relativePath())).append(NEWLINE);
                code.append(INDENT).append(" *").append(NEWLINE);
            }
            else {
                code.append(INDENT).append(" * Gets a generated statement.").append(NEWLINE);
                code.append(INDENT).append(" *").append(NEWLINE);
            }
            code.append(INDENT).append(" * @return statement model for this SQL source.").append(NEWLINE);
            code.append(INDENT).append(" */").append(NEWLINE);
            code.append(INDENT).append("public static ")
                .append(file.statement().getTopLevelInterface().getSimpleName()).append(" ")
                .append(file.methodName()).append("() {").append(NEWLINE);
            if (file.statement() instanceof SelectQuery) {
                code.append(INDENT).append(INDENT).append("var builder = SelectQuery.builder();").append(NEWLINE);
            }
            code.append(INDENT).append(INDENT).append("return ")
                .append(indentContinuationLines(statementExpression, 8))
                .append(";").append(NEWLINE);
            code.append(INDENT).append("}").append(NEWLINE).append(NEWLINE);
            code.append(INDENT).append("/**").append(NEWLINE);
            code.append(INDENT).append(" * Returns named parameters referenced by ").append(normalizePath(file.relativePath())).append(".").append(NEWLINE);
            code.append(INDENT).append(" *").append(NEWLINE);
            code.append(INDENT).append(" * @return immutable set of named parameter identifiers.").append(NEWLINE);
            code.append(INDENT).append(" */").append(NEWLINE);
            code.append(INDENT).append("public static Set<String> ").append(file.methodName()).append("Params() {").append(NEWLINE);
            code.append(INDENT).append(INDENT).append("return ").append(renderSetLiteral(file.parameters())).append(";").append(NEWLINE);
            code.append(INDENT).append("}").append(NEWLINE).append(NEWLINE);
        }
        code.append("}").append(NEWLINE);
        return code.toString();
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
}
