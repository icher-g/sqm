package io.sqm.transpile.builtin;

import io.sqm.core.*;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.core.transform.RecursiveNodeTransformer;
import io.sqm.transpile.RewriteFidelity;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.List;
import java.util.Set;

/**
 * Drops MySQL optimizer comments and table index hints for PostgreSQL transpilation and reports the loss as a warning.
 */
public final class MySqlToPostgresHintDroppingRule implements TranspileRule {
    /**
     * Creates a MySQL-to-PostgreSQL hint-dropping rule.
     */
    public MySqlToPostgresHintDroppingRule() {
    }

    @Override
    public String id() {
        return "mysql-to-postgres-hint-dropping";
    }

    @Override
    public Set<SqlDialectId> sourceDialects() {
        return Set.of(SqlDialectId.MYSQL);
    }

    @Override
    public Set<SqlDialectId> targetDialects() {
        return Set.of(SqlDialectId.POSTGRESQL);
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public TranspileRuleResult apply(Statement statement, TranspileContext context) {
        var transformer = new HintDroppingTransformer();
        var rewritten = transformer.transform(statement);
        if (rewritten == statement) {
            return TranspileRuleResult.unchanged(statement, "No MySQL optimizer or index hints detected");
        }
        return TranspileRuleResult.rewrittenWithWarning(
            rewritten,
            RewriteFidelity.EXACT,
            "MYSQL_HINTS_DROPPED",
            "MySQL optimizer comments and index hints were dropped during PostgreSQL transpilation",
            "Dropped MySQL optimizer comments and index hints"
        );
    }

    private static final class HintDroppingTransformer extends RecursiveNodeTransformer {
        private Statement transform(Statement statement) {
            return apply(statement);
        }

        @Override
        public Node visitSelectQuery(SelectQuery query) {
            var transformed = (SelectQuery) super.visitSelectQuery(query);
            if (transformed.optimizerHints().isEmpty()) {
                return transformed;
            }
            return SelectQuery.builder(transformed)
                .clearOptimizerHints()
                .build();
        }

        @Override
        public Node visitUpdateStatement(UpdateStatement statement) {
            var transformed = (UpdateStatement) super.visitUpdateStatement(statement);
            if (transformed.optimizerHints().isEmpty()) {
                return transformed;
            }
            return UpdateStatement.builder(transformed)
                .clearOptimizerHints()
                .build();
        }

        @Override
        public Node visitDeleteStatement(DeleteStatement statement) {
            var transformed = (DeleteStatement) super.visitDeleteStatement(statement);
            if (transformed.optimizerHints().isEmpty()) {
                return transformed;
            }
            return DeleteStatement.builder(transformed)
                .clearOptimizerHints()
                .build();
        }

        @Override
        public Node visitTable(Table table) {
            if (!table.indexHints().isEmpty()) {
                return table.withIndexHints(List.of());
            }
            return table;
        }
    }
}
