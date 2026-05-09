package io.sqm.transpile.builtin;

import io.sqm.core.dialect.SqlDialectId;
import io.sqm.dsl.Dsl;
import io.sqm.transpile.RewriteFidelity;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileOptions;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class HierarchicalQueryUnsupportedRuleTest {
    @Test
    void exposesGenericSourceAndUnsupportedTargets() {
        var rule = new HierarchicalQueryUnsupportedRule();

        assertEquals("hierarchical-query-unsupported", rule.id());
        assertEquals(Set.of(), rule.sourceDialects());
        assertEquals(Set.of(SqlDialectId.SQLSERVER), rule.targetDialects());
    }

    @Test
    void leavesStatementsWithoutHierarchicalQueriesUnchanged() {
        var statement = Dsl.select(Dsl.col("id")).from(Dsl.tbl("users")).build();
        var result = new HierarchicalQueryUnsupportedRule()
            .apply(statement, context(SqlDialectId.ORACLE, SqlDialectId.POSTGRESQL));

        assertFalse(result.changed());
        assertEquals(RewriteFidelity.EXACT, result.fidelity());
        assertTrue(result.problems().isEmpty());
    }

    @Test
    void rejectsHierarchicalQueriesForSqlServer() {
        var statement = select(col("id"))
            .from(tbl("categories"))
            .hierarchical(hierarchy(null, prior(col("id")).eq(col("parent_id")), false))
            .build();
        var result = new HierarchicalQueryUnsupportedRule()
            .apply(statement, context(SqlDialectId.ORACLE, SqlDialectId.SQLSERVER));

        assertEquals(RewriteFidelity.UNSUPPORTED, result.fidelity());
        assertEquals("UNSUPPORTED_HIERARCHICAL_QUERY", result.problems().getFirst().code());
    }

    private static TranspileContext context(SqlDialectId source, SqlDialectId target) {
        return new TranspileContext(source, target, TranspileOptions.defaults(), Optional.empty(), Optional.empty());
    }
}
