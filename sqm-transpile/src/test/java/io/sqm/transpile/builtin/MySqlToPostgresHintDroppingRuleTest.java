package io.sqm.transpile.builtin;

import io.sqm.core.DeleteStatement;
import io.sqm.core.SelectModifier;
import io.sqm.core.SelectQuery;
import io.sqm.core.UpdateStatement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.dsl.Dsl;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileOptions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySqlToPostgresHintDroppingRuleTest {
    private static final TranspileContext CONTEXT = new TranspileContext(
        SqlDialectId.MYSQL,
        SqlDialectId.POSTGRESQL,
        TranspileOptions.defaults(),
        Optional.empty(),
        Optional.empty()
    );

    @Test
    void leavesStatementsWithoutHintsUnchanged() {
        var statement = Dsl.select(Dsl.col("id")).from(Dsl.tbl("users")).build();

        var result = new MySqlToPostgresHintDroppingRule().apply(statement, CONTEXT);

        assertFalse(result.changed());
        assertSame(statement, result.statement());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void dropsHintsFromSelectWhilePreservingOtherShape() {
        var statement = SelectQuery.builder()
            .select(Dsl.col("id"))
            .from(Dsl.tbl("users").useIndex("idx_users_name"))
            .selectModifier(SelectModifier.CALC_FOUND_ROWS)
            .hint("MAX_EXECUTION_TIME", 1000)
            .build();

        var result = new MySqlToPostgresHintDroppingRule().apply(statement, CONTEXT);
        var rewritten = (SelectQuery) result.statement();

        assertTrue(result.changed());
        assertEquals("MYSQL_HINTS_DROPPED", result.warnings().getFirst().code());
        assertTrue(rewritten.hints().isEmpty());
        assertEquals(statement.items(), rewritten.items());
        assertTrue(((io.sqm.core.Table) rewritten.from()).hints().isEmpty());
    }

    @Test
    void dropsStatementHintsFromUpdateAndDelete() {
        var update = UpdateStatement.builder(Dsl.tbl("users").useIndex("idx_users_name"))
            .set(Dsl.set("name", Dsl.lit("alice")))
            .hint("BKA", "users")
            .build();
        var delete = DeleteStatement.builder(Dsl.tbl("users"))
            .using(Dsl.tbl("users").ignoreIndex("idx_users_name"))
            .hint("BKA", "users")
            .build();

        var updateResult = new MySqlToPostgresHintDroppingRule().apply(update, CONTEXT);
        var deleteResult = new MySqlToPostgresHintDroppingRule().apply(delete, CONTEXT);

        assertTrue(updateResult.changed());
        assertTrue(updateResult.statement().hints().isEmpty());
        assertTrue(((UpdateStatement) updateResult.statement()).table().hints().isEmpty());

        assertTrue(deleteResult.changed());
        assertTrue(deleteResult.statement().hints().isEmpty());
        assertTrue(((io.sqm.core.Table) ((DeleteStatement) deleteResult.statement()).using().getFirst()).hints().isEmpty());
    }

    @Test
    void dropsIndexHintsEvenWhenNoStatementOptimizerHintsExist() {
        var statement = Dsl.select(Dsl.col("id"))
            .from(Dsl.tbl("users").forceIndex("idx_users_name"))
            .build();

        var result = new MySqlToPostgresHintDroppingRule().apply(statement, CONTEXT);
        var rewritten = (SelectQuery) result.statement();

        assertTrue(result.changed());
        assertEquals("MYSQL_HINTS_DROPPED", result.warnings().getFirst().code());
        assertTrue(rewritten.hints().isEmpty());
        assertTrue(((io.sqm.core.Table) rewritten.from()).hints().isEmpty());
    }

    @Test
    void exposesRuleMetadataForRegistryFiltering() {
        var rule = new MySqlToPostgresHintDroppingRule();

        assertEquals("mysql-to-postgres-hint-dropping", rule.id());
        assertTrue(rule.sourceDialects().contains(SqlDialectId.MYSQL));
        assertTrue(rule.targetDialects().contains(SqlDialectId.POSTGRESQL));
        assertEquals(100, rule.order());
    }
}

