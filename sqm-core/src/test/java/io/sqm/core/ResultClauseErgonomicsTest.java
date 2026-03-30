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
import static io.sqm.dsl.Dsl.resultInto;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.tableVar;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultClauseErgonomicsTest {

    @Test
    void exposesSemanticHelpersForReturningAndOutputStyles() {
        var returning = result(col("id"));
        var output = result(inserted("id"), deleted("name").as("old_name"));
        var outputInto = result(resultInto(tbl("audit_log"), "new_id"), insertedAll(), deletedAll());

        assertFalse(returning.hasIntoTarget());
        assertFalse(returning.usesDialectSpecificResultItems());

        assertFalse(output.hasIntoTarget());
        assertTrue(output.usesDialectSpecificResultItems());

        assertTrue(outputInto.hasIntoTarget());
        assertTrue(outputInto.usesDialectSpecificResultItems());
    }

    @Test
    void classifiesResultIntoTargetsBySemanticCategory() {
        var baseTable = resultInto(tbl("audit_log"), "id");
        var variableTable = resultInto(tableVar("@audit_rows"), "id");
        var derived = resultInto(tbl(select(lit(1L)).build()).as("audit_rows"), id("id"));

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
            .result(resultInto(tableVar("@audit_rows"), "user_id"), inserted("id"))
            .build();

        assertTrue(statement.result().hasIntoTarget());
        assertTrue(statement.result().usesDialectSpecificResultItems());
        assertTrue(statement.result().into().isVariableTarget());
    }
}
