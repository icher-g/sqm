package io.sqm.transpile.builtin;

import io.sqm.core.BinaryOperatorExpr;
import io.sqm.core.DeleteStatement;
import io.sqm.core.FunctionExpr;
import io.sqm.core.InsertStatement;
import io.sqm.core.LikeMode;
import io.sqm.core.MergeStatement;
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
        assertTrue(StatementFeatureInspector.hasLikeMode(ilike, LikeMode.ILIKE));
        assertTrue(StatementFeatureInspector.hasAnyBinaryOperator(operator, Set.of("->>")));

        assertFalse(StatementFeatureInspector.hasResultClause(Dsl.select(Dsl.col("id")).from(Dsl.tbl("users")).build()));
        assertFalse(StatementFeatureInspector.hasDistinctOn(Dsl.select(Dsl.col("id")).from(Dsl.tbl("users")).build()));
        assertFalse(StatementFeatureInspector.hasLikeMode(ilike, LikeMode.LIKE));
        assertFalse(StatementFeatureInspector.hasAnyBinaryOperator(operator, Set.of("@>")));
    }

    @Test
    void distinguishesGenericResultClausesFromSqlServerSpecificOutput() {
        var returningInsert = InsertStatement.builder(Dsl.tbl("users"))
            .columns(Dsl.id("id"))
            .values(Dsl.rows(Dsl.row(Dsl.lit(1))))
            .result(Dsl.col("id").toSelectItem())
            .build();
        var outputUpdate = UpdateStatement.builder(Dsl.tbl("users"))
            .set(Dsl.set("name", Dsl.lit("alice")))
            .result(Dsl.deleted("name"), Dsl.inserted("name"))
            .build();
        var outputIntoUpdate = UpdateStatement.builder(Dsl.tbl("users"))
            .set(Dsl.set("name", Dsl.lit("alice")))
            .result(Dsl.resultInto(Dsl.tbl("audit"), "old_name"), Dsl.deleted("name"))
            .build();
        var outputMerge = Dsl.merge("users")
            .source(Dsl.tbl("src").as("s"))
            .on(Dsl.col("users", "id").eq(Dsl.col("s", "id")))
            .whenMatchedDelete()
            .result(Dsl.deleted("id"))
            .build();
        var outputDeleteAll = DeleteStatement.builder(Dsl.tbl("users"))
            .result(Dsl.deletedAll())
            .build();

        assertTrue(StatementFeatureInspector.hasResultClause(returningInsert));
        assertFalse(StatementFeatureInspector.hasSqlServerOutputClause(returningInsert));
        assertTrue(StatementFeatureInspector.hasSqlServerOutputClause(outputUpdate));
        assertTrue(StatementFeatureInspector.hasSqlServerOutputClause(outputIntoUpdate));
        assertTrue(StatementFeatureInspector.hasResultClause(outputMerge));
        assertTrue(StatementFeatureInspector.hasSqlServerOutputClause(outputMerge));
        assertTrue(StatementFeatureInspector.hasSqlServerOutputClause(outputDeleteAll));
    }

    @Test
    void detectsFunctionPrefixesInsertModesAndConflictActions() {
        var functionQuery = Dsl.select(FunctionExpr.of(
                QualifiedName.of("json_extract"),
                List.of(FunctionExpr.Arg.expr(Dsl.col("payload")), FunctionExpr.Arg.expr(Dsl.lit("$.id"))),
                false,
                null,
                null,
                null
            ))
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

        assertTrue(StatementFeatureInspector.hasAnyFunctionNamePrefix(functionQuery, Set.of("json_")));
        assertTrue(StatementFeatureInspector.hasInsertMode(insertIgnore, InsertStatement.InsertMode.IGNORE));
        assertTrue(StatementFeatureInspector.hasOnConflictAction(insertConflict, InsertStatement.OnConflictAction.DO_UPDATE));

        assertFalse(StatementFeatureInspector.hasAnyFunctionNamePrefix(functionQuery, Set.of("DATE_")));
        assertFalse(StatementFeatureInspector.hasInsertMode(insertIgnore, InsertStatement.InsertMode.REPLACE));
        assertFalse(StatementFeatureInspector.hasOnConflictAction(insertIgnore, InsertStatement.OnConflictAction.DO_UPDATE));
    }

    @Test
    void detectsMergeStatements() {
        MergeStatement mergeStatement = Dsl.merge("users")
            .source(Dsl.tbl("src").as("s"))
            .on(Dsl.col("users", "id").eq(Dsl.col("s", "id")))
            .whenMatchedDelete()
            .build();

        assertTrue(StatementFeatureInspector.hasMergeStatement(mergeStatement));
        assertFalse(StatementFeatureInspector.hasMergeStatement(Dsl.select(Dsl.col("id")).from(Dsl.tbl("users")).build()));
    }
}
