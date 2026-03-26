package io.sqm.transpile.builtin;

import io.sqm.core.DeleteStatement;
import io.sqm.core.InsertStatement;
import io.sqm.core.MergeClause;
import io.sqm.core.MergeDeleteAction;
import io.sqm.core.MergeStatement;
import io.sqm.core.SelectModifier;
import io.sqm.core.SelectQuery;
import io.sqm.core.StatementHint;
import io.sqm.core.UpdateStatement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.dsl.Dsl;
import io.sqm.transpile.RewriteFidelity;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileOptions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySqlHintDroppingRuleTest {
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

        var result = new MySqlHintDroppingRule().apply(statement, CONTEXT);

        assertFalse(result.changed());
        assertSame(statement, result.statement());
        assertEquals(RewriteFidelity.EXACT, result.fidelity());
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

        var result = new MySqlHintDroppingRule().apply(statement, CONTEXT);
        var rewritten = (SelectQuery) result.statement();

        assertTrue(result.changed());
        assertEquals(RewriteFidelity.APPROXIMATE, result.fidelity());
        assertEquals("MYSQL_HINTS_DROPPED", result.warnings().getFirst().code());
        assertTrue(rewritten.hints().isEmpty());
        assertEquals(statement.items(), rewritten.items());
        assertTrue(((io.sqm.core.Table) rewritten.from()).hints().isEmpty());
    }

    @Test
    void dropsStatementHintsFromUpdateDeleteInsertAndMerge() {
        var update = UpdateStatement.builder(Dsl.tbl("users").useIndex("idx_users_name"))
            .set(Dsl.set("name", Dsl.lit("alice")))
            .hint("BKA", "users")
            .build();
        var delete = DeleteStatement.builder(Dsl.tbl("users"))
            .using(Dsl.tbl("users").ignoreIndex("idx_users_name"))
            .hint("BKA", "users")
            .build();
        var insert = InsertStatement.builder(Dsl.tbl("users"))
            .columns(Dsl.id("id"))
            .values(Dsl.rows(Dsl.row(Dsl.lit(1))))
            .hint("QB_NAME", Dsl.lit("main"))
            .build();
        var merge = MergeStatement.of(
            Dsl.tbl("users"),
            Dsl.tbl("src").as("s"),
            Dsl.col("users", "id").eq(Dsl.col("s", "id")),
            null,
            List.of(MergeClause.of(MergeClause.MatchType.MATCHED, MergeDeleteAction.of())),
            null,
            List.of(StatementHint.of("MERGE_HINT"))
        );

        var updateResult = new MySqlHintDroppingRule().apply(update, CONTEXT);
        var deleteResult = new MySqlHintDroppingRule().apply(delete, CONTEXT);
        var insertResult = new MySqlHintDroppingRule().apply(insert, CONTEXT);
        var mergeResult = new MySqlHintDroppingRule().apply(merge, CONTEXT);

        assertTrue(updateResult.changed());
        assertEquals(RewriteFidelity.APPROXIMATE, updateResult.fidelity());
        assertTrue(updateResult.statement().hints().isEmpty());
        assertTrue(((UpdateStatement) updateResult.statement()).table().hints().isEmpty());

        assertTrue(deleteResult.changed());
        assertEquals(RewriteFidelity.APPROXIMATE, deleteResult.fidelity());
        assertTrue(deleteResult.statement().hints().isEmpty());
        assertTrue(((io.sqm.core.Table) ((DeleteStatement) deleteResult.statement()).using().getFirst()).hints().isEmpty());

        assertTrue(insertResult.changed());
        assertEquals(RewriteFidelity.APPROXIMATE, insertResult.fidelity());
        assertTrue(insertResult.statement().hints().isEmpty());

        assertTrue(mergeResult.changed());
        assertEquals(RewriteFidelity.APPROXIMATE, mergeResult.fidelity());
        assertTrue(mergeResult.statement().hints().isEmpty());
    }

    @Test
    void dropsGenericMysqlHintModelsToo() {
        var statement = Dsl.select(Dsl.col("id"))
            .from(Dsl.tbl("users").hint("NOLOCK"))
            .hint("JOIN_ORDER", "users")
            .build();

        var result = new MySqlHintDroppingRule().apply(statement, CONTEXT);
        var rewritten = (SelectQuery) result.statement();

        assertTrue(result.changed());
        assertEquals(RewriteFidelity.APPROXIMATE, result.fidelity());
        assertEquals("MYSQL_HINTS_DROPPED", result.warnings().getFirst().code());
        assertTrue(rewritten.hints().isEmpty());
        assertTrue(((io.sqm.core.Table) rewritten.from()).hints().isEmpty());
    }

    @Test
    void exposesRuleMetadataForRegistryFiltering() {
        var rule = new MySqlHintDroppingRule();

        assertEquals("mysql-hint-dropping", rule.id());
        assertTrue(rule.sourceDialects().contains(SqlDialectId.MYSQL));
        assertTrue(rule.targetDialects().contains(SqlDialectId.POSTGRESQL));
        assertTrue(rule.targetDialects().contains(SqlDialectId.SQLSERVER));
        assertTrue(rule.targetDialects().contains(SqlDialectId.ANSI));
        assertEquals(100, rule.order());
    }
}
