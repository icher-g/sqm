package io.sqm.transpile.builtin;

import io.sqm.core.Statement;
import io.sqm.core.TableSampleSpec;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.RewriteFidelity;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileOptions;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class TableAccessModifierUnsupportedRuleTest {
    @Test
    void exposesGlobalRuntimeGuardShape() {
        var rule = new TableAccessModifierUnsupportedRule();

        assertEquals("table-access-modifier-unsupported", rule.id());
        assertEquals(Set.of(), rule.sourceDialects());
        assertEquals(Set.of(), rule.targetDialects());
    }

    @Test
    void leavesStatementsWithoutTableAccessModifiersUnchanged() {
        var statement = select(col("id")).from(tbl("users")).build();
        var result = new TableAccessModifierUnsupportedRule()
            .apply(statement, context(SqlDialectId.ORACLE, SqlDialectId.POSTGRESQL));

        assertFalse(StatementFeatureInspector.hasTableAccessModifier(statement));
        assertFalse(result.changed());
        assertEquals(RewriteFidelity.EXACT, result.fidelity());
        assertTrue(result.problems().isEmpty());
    }

    @Test
    void leavesSameDialectTableAccessModifiersUnchanged() {
        var statement = versionedStatement();
        var result = new TableAccessModifierUnsupportedRule()
            .apply(statement, context(SqlDialectId.ORACLE, SqlDialectId.ORACLE));

        assertTrue(StatementFeatureInspector.hasTableAccessModifier(statement));
        assertFalse(result.changed());
        assertEquals(RewriteFidelity.EXACT, result.fidelity());
        assertTrue(result.problems().isEmpty());
    }

    @Test
    void rejectsCrossDialectTableVersionPartitionAndSampling() {
        var versioned = new TableAccessModifierUnsupportedRule()
            .apply(versionedStatement(), context(SqlDialectId.ORACLE, SqlDialectId.POSTGRESQL));
        var partitioned = new TableAccessModifierUnsupportedRule()
            .apply(select(star()).from(tbl("sales").withPartitionSpec(tablePartition("sales_q1"))).build(),
                context(SqlDialectId.ORACLE, SqlDialectId.SQLSERVER));
        var sampled = new TableAccessModifierUnsupportedRule()
            .apply(select(star()).from(sampled(
                tbl("users"),
                tableSample(TableSampleSpec.SampleMethod.SYSTEM, TableSampleSpec.SampleUnit.PERCENT, lit(10), lit(42)))).build(),
                context(SqlDialectId.POSTGRESQL, SqlDialectId.MYSQL));

        assertUnsupported(versioned);
        assertUnsupported(partitioned);
        assertUnsupported(sampled);
    }

    private static Statement versionedStatement() {
        return select(star())
            .from(tbl("orders").withVersion(asOfTimestamp(lit(42))))
            .build();
    }

    private static void assertUnsupported(io.sqm.transpile.TranspileRuleResult result) {
        assertEquals(RewriteFidelity.UNSUPPORTED, result.fidelity());
        assertEquals("UNSUPPORTED_TABLE_ACCESS_MODIFIER", result.problems().getFirst().code());
    }

    private static TranspileContext context(SqlDialectId source, SqlDialectId target) {
        return new TranspileContext(source, target, TranspileOptions.defaults(), Optional.empty(), Optional.empty());
    }
}
