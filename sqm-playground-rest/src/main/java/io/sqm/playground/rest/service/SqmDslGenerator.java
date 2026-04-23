package io.sqm.playground.rest.service;

import io.sqm.codegen.*;
import io.sqm.core.NamedParamExpr;
import io.sqm.core.Node;
import io.sqm.core.Statement;
import io.sqm.core.StatementSequence;
import io.sqm.core.walk.RecursiveNodeVisitor;
import io.sqm.playground.api.SqlDialectDto;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Generates Java DSL source for SQM statements returned by the playground API.
 */
@Service
public class SqmDslGenerator {

    /**
     * Constructs a new instance of the class.
     */
    public SqmDslGenerator() {

    }

    private static final String CLASS_NAME = "MyStatement";

    private static SqlCodegenDialect toCodegenDialect(SqlDialectDto dialect) {
        Objects.requireNonNull(dialect, "dialect must not be null");
        return switch (dialect) {
            case ansi -> SqlCodegenDialect.ANSI;
            case postgresql -> SqlCodegenDialect.POSTGRESQL;
            case mysql -> SqlCodegenDialect.MYSQL;
            case sqlserver -> SqlCodegenDialect.SQLSERVER;
        };
    }

    private static SqlFolderGroup toGroup(Path path, Node node) {
        var collector = new NamedParametersCollector();
        node.accept(collector);
        var statements = node instanceof Statement statement ? List.of(statement) : ((StatementSequence) node).statements();
        return new SqlFolderGroup(path, CLASS_NAME, List.of(new SqlSourceFile(path, path, "getStatement", collector.parameters, "", statements)));
    }

    /**
     * Generates Java DSL source for the given statement using the requested dialect defaults.
     *
     * @param node    parsed SQM statement or sequence of statements
     * @param dialect source dialect used for code generation settings
     * @return generated Java DSL source
     */
    public String toDsl(Node node, SqlDialectDto dialect) {
        var empty = Path.of("sqm-playground");
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
        return renderer.render(toGroup(empty, node));
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
