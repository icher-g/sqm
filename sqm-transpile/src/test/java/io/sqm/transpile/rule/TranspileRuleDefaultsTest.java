package io.sqm.transpile.rule;

import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.dsl.Dsl;
import io.sqm.transpile.RewriteFidelity;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileOptions;
import io.sqm.transpile.TranspileRuleResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranspileRuleDefaultsTest {

    @Test
    void defaultSupportsUsesDialectSetsForPairAndContext() {
        TranspileRule rule = new TranspileRule() {
            @Override
            public String id() {
                return "dialect-filter";
            }

            @Override
            public Set<SqlDialectId> sourceDialects() {
                return Set.of(SqlDialectId.ANSI);
            }

            @Override
            public Set<SqlDialectId> targetDialects() {
                return Set.of(SqlDialectId.MYSQL);
            }

            @Override
            public TranspileRuleResult apply(Statement statement, TranspileContext context) {
                return TranspileRuleResult.rewritten(statement, RewriteFidelity.EXACT, "applied");
            }
        };

        assertTrue(rule.supports(SqlDialectId.ANSI, SqlDialectId.MYSQL));
        assertFalse(rule.supports(SqlDialectId.POSTGRESQL, SqlDialectId.MYSQL));
        assertTrue(rule.supports(new TranspileContext(
            SqlDialectId.ANSI,
            SqlDialectId.MYSQL,
            TranspileOptions.defaults(),
            Optional.empty(),
            Optional.empty()
        )));
    }

    @Test
    void defaultRegistryOrdersRulesByOrderThenId() {
        var statement = Dsl.select(Dsl.lit(1)).build();
        TranspileRule later = rule("z-rule", 10, statement);
        TranspileRule earlier = rule("a-rule", 10, statement);
        TranspileRule first = rule("first", 0, statement);

        var rules = DefaultTranspileRuleRegistry.of(List.of(later, first, earlier))
            .rulesFor(SqlDialectId.ANSI, SqlDialectId.MYSQL);

        assertEquals(List.of("first", "a-rule", "z-rule"), rules.stream().map(TranspileRule::id).toList());
    }

    private static TranspileRule rule(String id, int order, Statement statement) {
        return new TranspileRule() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public Set<SqlDialectId> sourceDialects() {
                return Set.of();
            }

            @Override
            public Set<SqlDialectId> targetDialects() {
                return Set.of();
            }

            @Override
            public int order() {
                return order;
            }

            @Override
            public TranspileRuleResult apply(Statement ignored, TranspileContext context) {
                return TranspileRuleResult.unchanged(statement, id);
            }
        };
    }
}

