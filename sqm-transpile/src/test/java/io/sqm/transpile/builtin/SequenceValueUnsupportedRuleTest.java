package io.sqm.transpile.builtin;

import io.sqm.core.dialect.SqlDialectId;
import io.sqm.dsl.Dsl;
import io.sqm.transpile.RewriteFidelity;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileOptions;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SequenceValueUnsupportedRuleTest {
    @Test
    void exposesGenericSourceAndUnsupportedTargets() {
        var rule = new SequenceValueUnsupportedRule();

        assertEquals("sequence-value-unsupported", rule.id());
        assertEquals(Set.of(), rule.sourceDialects());
        assertEquals(Set.of(SqlDialectId.ANSI, SqlDialectId.MYSQL, SqlDialectId.SQLSERVER), rule.targetDialects());
    }

    @Test
    void leavesStatementsWithoutSequenceValuesUnchanged() {
        var statement = Dsl.select(Dsl.col("id")).from(Dsl.tbl("users")).build();
        var result = new SequenceValueUnsupportedRule()
            .apply(statement, context(SqlDialectId.ORACLE, SqlDialectId.MYSQL));

        assertFalse(result.changed());
        assertEquals(RewriteFidelity.EXACT, result.fidelity());
        assertTrue(result.problems().isEmpty());
    }

    @Test
    void allowsNextSequenceValuesForSqlServerTargets() {
        var statement = Dsl.select(Dsl.nextValue("users_seq")).from(Dsl.tbl("users")).build();
        var result = new SequenceValueUnsupportedRule()
            .apply(statement, context(SqlDialectId.ORACLE, SqlDialectId.SQLSERVER));

        assertFalse(result.changed());
        assertEquals(RewriteFidelity.EXACT, result.fidelity());
        assertTrue(result.problems().isEmpty());
    }

    @Test
    void rejectsCurrentSequenceValuesForSqlServerTargets() {
        var statement = Dsl.select(Dsl.currentValue("users_seq")).from(Dsl.tbl("users")).build();
        var result = new SequenceValueUnsupportedRule()
            .apply(statement, context(SqlDialectId.ORACLE, SqlDialectId.SQLSERVER));

        assertEquals(RewriteFidelity.UNSUPPORTED, result.fidelity());
        assertEquals("UNSUPPORTED_SEQUENCE_CURRENT_VALUE", result.problems().getFirst().code());
    }

    @Test
    void rejectsSequenceValuesForUnsupportedTargets() {
        var statement = Dsl.select(Dsl.nextValue("users_seq")).from(Dsl.tbl("users")).build();
        var result = new SequenceValueUnsupportedRule()
            .apply(statement, context(SqlDialectId.ORACLE, SqlDialectId.MYSQL));

        assertEquals(RewriteFidelity.UNSUPPORTED, result.fidelity());
        assertEquals("UNSUPPORTED_SEQUENCE_VALUE", result.problems().getFirst().code());
    }

    private static TranspileContext context(SqlDialectId source, SqlDialectId target) {
        return new TranspileContext(source, target, TranspileOptions.defaults(), Optional.empty(), Optional.empty());
    }
}
