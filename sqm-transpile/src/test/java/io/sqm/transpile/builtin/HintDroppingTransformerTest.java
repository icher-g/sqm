package io.sqm.transpile.builtin;

import io.sqm.core.DeleteStatement;
import io.sqm.core.InsertStatement;
import io.sqm.core.MergeStatement;
import io.sqm.core.SelectQuery;
import io.sqm.core.UpdateStatement;
import io.sqm.dsl.Dsl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HintDroppingTransformerTest {

    @Test
    void transform_returnsSameStatementWhenNoHintsExist() {
        var statement = Dsl.select(Dsl.col("id"))
            .from(Dsl.tbl("users"))
            .build();

        var transformed = new HintDroppingTransformer().transform(statement);

        assertSame(statement, transformed);
    }

    @Test
    void transform_dropsStatementAndTableHintsAcrossStatementFamilies() {
        var select = Dsl.select(Dsl.col("id"))
            .from(Dsl.tbl("users").hint("USE_INDEX", "idx_users_name"))
            .hint("MAX_EXECUTION_TIME", 1000)
            .build();
        var update = UpdateStatement.builder(Dsl.tbl("users").hint("NOLOCK"))
            .set(Dsl.set("name", Dsl.lit("alice")))
            .hint("QB_NAME", "main")
            .build();
        var delete = DeleteStatement.builder(Dsl.tbl("users").hint("NOLOCK"))
            .hint("QB_NAME", "main")
            .build();
        var insert = InsertStatement.builder(Dsl.tbl("users"))
            .columns(Dsl.id("id"))
            .values(Dsl.rows(Dsl.row(Dsl.lit(1))))
            .hint("QB_NAME", "main")
            .build();
        var merge = Dsl.merge("users")
            .source(Dsl.tbl("users").as("src").hint("NOLOCK"))
            .on(Dsl.col("users", "id").eq(Dsl.col("src", "id")))
            .whenMatchedDelete()
            .hint("QB_NAME", "main")
            .build();

        var transformer = new HintDroppingTransformer();

        var transformedSelect = (SelectQuery) transformer.transform(select);
        var transformedUpdate = (UpdateStatement) transformer.transform(update);
        var transformedDelete = (DeleteStatement) transformer.transform(delete);
        var transformedInsert = (InsertStatement) transformer.transform(insert);
        var transformedMerge = (MergeStatement) transformer.transform(merge);

        assertTrue(transformedSelect.hints().isEmpty());
        assertTrue(((io.sqm.core.Table) transformedSelect.from()).hints().isEmpty());
        assertTrue(transformedUpdate.hints().isEmpty());
        assertTrue(transformedUpdate.table().hints().isEmpty());
        assertTrue(transformedDelete.hints().isEmpty());
        assertTrue(transformedDelete.table().hints().isEmpty());
        assertTrue(transformedInsert.hints().isEmpty());
        assertTrue(transformedMerge.hints().isEmpty());
        assertTrue(((io.sqm.core.Table) transformedMerge.source()).hints().isEmpty());
    }
}
