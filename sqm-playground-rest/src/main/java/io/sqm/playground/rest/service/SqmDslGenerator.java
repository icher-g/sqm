package io.sqm.playground.rest.service;

import io.sqm.codegen.*;
import io.sqm.core.NamedParamExpr;
import io.sqm.core.Statement;
import io.sqm.core.walk.RecursiveNodeVisitor;
import io.sqm.parser.core.ParserException;
import io.sqm.playground.api.SqlDialectDto;
import org.springframework.stereotype.Service;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import java.nio.file.Path;
import java.util.*;

/**
 * Generates Java DSL source for SQM statements returned by the playground API.
 */
@Service
public class SqmDslGenerator {

    private static final String CLASS_NAME = "MyStatement";

    private final InMemoryCompiler compiler;

    public SqmDslGenerator(InMemoryCompiler compiler) {
        this.compiler = compiler;
    }

    private static SqlCodegenDialect toCodegenDialect(SqlDialectDto dialect) {
        Objects.requireNonNull(dialect, "dialect must not be null");
        return switch (dialect) {
            case ansi -> SqlCodegenDialect.ANSI;
            case postgresql -> SqlCodegenDialect.POSTGRESQL;
            case mysql -> SqlCodegenDialect.MYSQL;
            case sqlserver -> SqlCodegenDialect.SQLSERVER;
        };
    }

    private static SqlFolderGroup toGroup(Path path, Statement statement) {
        var collector = new NamedParametersCollector();
        statement.accept(collector);
        return new SqlFolderGroup(path, CLASS_NAME, List.of(new SqlSourceFile(path, path, "getStatement", statement, collector.parameters, "")));
    }

    /**
     * Generates Java DSL source for the given statement using the requested dialect defaults.
     *
     * @param statement parsed SQM statement
     * @param dialect   source dialect used for code generation settings
     * @return generated Java DSL source
     */
    public String toDsl(Statement statement, SqlDialectDto dialect) {
        var empty = Path.of("provided-by-user");
        SqlFileCodegenOptions options = SqlFileCodegenOptions.of(
            empty,
            empty,
            "sqm.codegen",
            toCodegenDialect(dialect),
            false,
            false,
            null,
            false
        );

        var renderer = new SqmDslRenderer(options);
        var dslCode = renderer.render(toGroup(empty, statement));
        var diagnostics = new DiagnosticCollector<JavaFileObject>();

        // compile the generated code to validate its correctness.
        boolean success = compiler.compile(CLASS_NAME, dslCode, diagnostics);
        if (!success) {
            if (diagnostics.getDiagnostics().isEmpty()) {
                throw new ParserException("Unexpected compilation error.", -1);
            }
            var diagnostic = diagnostics.getDiagnostics().getFirst();
            throw new ParserException(diagnostic.getMessage(Locale.ENGLISH), (int) diagnostic.getPosition(), (int) diagnostic.getLineNumber(), (int) diagnostic.getColumnNumber());
        }
        return dslCode;
    }

    private static final class NamedParametersCollector extends RecursiveNodeVisitor<Void> {

        private final Set<String> parameters = new HashSet<>();

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitNamedParamExpr(NamedParamExpr p) {
            parameters.add(p.name());
            return super.visitNamedParamExpr(p);
        }
    }
}
