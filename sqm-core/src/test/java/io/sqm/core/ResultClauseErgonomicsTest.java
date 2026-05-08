package io.sqm.core;

import io.sqm.dsl.Dsl;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.deleted;
import static io.sqm.dsl.Dsl.deletedAll;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.inserted;
import static io.sqm.dsl.Dsl.insertedAll;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.result;
import static io.sqm.dsl.Dsl.resultRelationTarget;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.tableVar;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultClauseErgonomicsTest {

    @Test
    void exposesSemanticHelpersForReturningAndOutputStyles() {
        var returning = result(col("id"));
        var output = result(inserted("id"), deleted("name").as("old_name"));
        var outputInto = result(resultRelationTarget(tbl("audit_log"), "new_id"), insertedAll(), deletedAll());

        assertFalse(returning.target() instanceof RelationResultTarget);
        assertFalse(returning.usesDialectSpecificResultItems());

        assertFalse(output.target() instanceof RelationResultTarget);
        assertTrue(output.usesDialectSpecificResultItems());

        assertInstanceOf(RelationResultTarget.class, outputInto.target());
        assertTrue(outputInto.usesDialectSpecificResultItems());
    }

    @Test
    void classifiesRelationResultTargetTargetsBySemanticCategory() {
        var baseTable = resultRelationTarget(tbl("audit_log"), "id");
        var variableTable = resultRelationTarget(tableVar("@audit_rows"), "id");
        var derived = resultRelationTarget(tbl(select(lit(1L)).build()).as("audit_rows"), id("id"));

        assertTrue(baseTable.isBaseTableTarget());
        assertFalse(baseTable.isVariableTarget());
        assertFalse(baseTable.isDerivedTarget());

        assertFalse(variableTable.isBaseTableTarget());
        assertTrue(variableTable.isVariableTarget());
        assertFalse(variableTable.isDerivedTarget());

        assertFalse(derived.isBaseTableTarget());
        assertFalse(derived.isVariableTarget());
        assertTrue(derived.isDerivedTarget());
    }

    @Test
    void supportsCanonicalResultClauseInspectionStyleInTransformScenarios() {
        var statement = Dsl.update("users")
            .set(id("name"), lit("alice"))
            .result(resultRelationTarget(tableVar("@audit_rows"), "user_id"), inserted("id"))
            .build();

        var target = assertInstanceOf(RelationResultTarget.class, statement.result().target());
        assertTrue(statement.result().usesDialectSpecificResultItems());
        assertTrue(target.isVariableTarget());
    }
}
