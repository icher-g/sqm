package io.sqm.transpile.builtin;

import io.sqm.core.BinaryOperatorExpr;
import io.sqm.core.DeleteStatement;
import io.sqm.core.FunctionExpr;
import io.sqm.core.InsertStatement;
import io.sqm.core.LikeMode;
import io.sqm.core.QualifiedName;
import io.sqm.core.SelectQuery;
import io.sqm.core.UpdateStatement;
import io.sqm.dsl.Dsl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatementFeatureInspectorTest {

    @Test
    void detectsResultClauseDistinctOnLikeModesAndBinaryOperators() {
        var insert = InsertStatement.builder(Dsl.tbl("users"))
            .columns(Dsl.id("id"))
            .values(Dsl.rows(Dsl.row(Dsl.lit(1))))
            .result(Dsl.col("id").toSelectItem())
            .build();
        var update = UpdateStatement.builder(Dsl.tbl("users"))
            .set(Dsl.set("name", Dsl.lit("alice")))
            .result(Dsl.col("id").toSelectItem())
            .build();
        var delete = DeleteStatement.builder(Dsl.tbl("users"))
            .result(Dsl.col("id").toSelectItem())
            .build();
        var distinctOn = SelectQuery.builder()
            .select(Dsl.col("name"))
            .from(Dsl.tbl("users"))
            .distinct(Dsl.col("user_id"))
            .build();
        var topSpec = SelectQuery.builder()
            .select(Dsl.col("id"))
            .from(Dsl.tbl("users"))
            .top(Dsl.lit(5))
            .build();
        var ilike = Dsl.select(Dsl.col("name"))
            .from(Dsl.tbl("users"))
            .where(Dsl.col("name").ilike("al%"))
            .build();
        var operator = Dsl.select(BinaryOperatorExpr.of(Dsl.col("payload"), io.sqm.core.OperatorName.of("->>"), Dsl.lit("name")))
            .from(Dsl.tbl("users"))
            .build();

        assertTrue(StatementFeatureInspector.hasResultClause(insert));
        assertTrue(StatementFeatureInspector.hasResultClause(update));
        assertTrue(StatementFeatureInspector.hasResultClause(delete));
        assertTrue(StatementFeatureInspector.hasDistinctOn(distinctOn));
        assertTrue(StatementFeatureInspector.hasTopSpec(topSpec));
        assertTrue(StatementFeatureInspector.hasLikeMode(ilike, LikeMode.ILIKE));
        assertTrue(StatementFeatureInspector.hasAnyBinaryOperator(operator, Set.of("->>")));

        assertFalse(StatementFeatureInspector.hasResultClause(Dsl.select(Dsl.col("id")).from(Dsl.tbl("users")).build()));
        assertFalse(StatementFeatureInspector.hasDistinctOn(Dsl.select(Dsl.col("id")).from(Dsl.tbl("users")).build()));
        assertFalse(StatementFeatureInspector.hasTopSpec(Dsl.select(Dsl.col("id")).from(Dsl.tbl("users")).build()));
        assertFalse(StatementFeatureInspector.hasLikeMode(ilike, LikeMode.LIKE));
        assertFalse(StatementFeatureInspector.hasAnyBinaryOperator(operator, Set.of("@>")));
    }

    @Test
    void detectsFunctionsHintsInsertModesAndConflictActions() {
        var functionQuery = Dsl.select(FunctionExpr.of(
                QualifiedName.of("json_extract"),
                List.of(FunctionExpr.Arg.expr(Dsl.col("payload")), FunctionExpr.Arg.expr(Dsl.lit("$.id"))),
                false,
                null,
                null,
                null
            ))
            .from(Dsl.tbl("users").useIndex("idx_users_name"))
            .build();
        var hintedSelect = SelectQuery.builder()
            .select(Dsl.col("id"))
            .from(Dsl.tbl("users"))
            .optimizerHint("MAX_EXECUTION_TIME(1000)")
            .build();
        var hintedUpdate = UpdateStatement.builder(Dsl.tbl("users"))
            .set(Dsl.set("name", Dsl.lit("alice")))
            .optimizerHint("BKA(users)")
            .build();
        var hintedDelete = DeleteStatement.builder(Dsl.tbl("users"))
            .optimizerHint("BKA(users)")
            .build();
        var insertIgnore = InsertStatement.builder(Dsl.tbl("users"))
            .ignore()
            .columns(Dsl.id("id"))
            .values(Dsl.rows(Dsl.row(Dsl.lit(1))))
            .build();
        var insertConflict = InsertStatement.builder(Dsl.tbl("users"))
            .columns(Dsl.id("id"))
            .values(Dsl.rows(Dsl.row(Dsl.lit(1))))
            .onConflictDoUpdate(
                List.of(Dsl.id("id")),
                List.of(Dsl.set("name", Dsl.lit("alice"))),
                null
            )
            .build();

        assertTrue(StatementFeatureInspector.hasAnyFunctionName(functionQuery, Set.of("JSON_EXTRACT")));
        assertTrue(StatementFeatureInspector.hasAnyFunctionNamePrefix(functionQuery, Set.of("json_")));
        assertTrue(StatementFeatureInspector.hasOptimizerHints(hintedSelect));
        assertTrue(StatementFeatureInspector.hasOptimizerHints(hintedUpdate));
        assertTrue(StatementFeatureInspector.hasOptimizerHints(hintedDelete));
        assertTrue(StatementFeatureInspector.hasIndexHints(functionQuery));
        assertTrue(StatementFeatureInspector.hasInsertMode(insertIgnore, InsertStatement.InsertMode.IGNORE));
        assertTrue(StatementFeatureInspector.hasOnConflictAction(insertConflict, InsertStatement.OnConflictAction.DO_UPDATE));

        assertFalse(StatementFeatureInspector.hasAnyFunctionName(functionQuery, Set.of("JSON_OBJECT")));
        assertFalse(StatementFeatureInspector.hasAnyFunctionNamePrefix(functionQuery, Set.of("DATE_")));
        assertFalse(StatementFeatureInspector.hasOptimizerHints(functionQuery));
        assertFalse(StatementFeatureInspector.hasIndexHints(hintedSelect));
        assertFalse(StatementFeatureInspector.hasInsertMode(insertIgnore, InsertStatement.InsertMode.REPLACE));
        assertFalse(StatementFeatureInspector.hasOnConflictAction(insertIgnore, InsertStatement.OnConflictAction.DO_UPDATE));
    }
}
