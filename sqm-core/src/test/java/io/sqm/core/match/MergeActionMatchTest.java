package io.sqm.core.match;

import io.sqm.core.MergeAction;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.set;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MergeActionMatchTest {

    @Test
    void matchesUpdateDeleteAndInsertActions() {
        MergeAction update = io.sqm.core.MergeUpdateAction.of(java.util.List.of(set("name", lit("alice"))));
        MergeAction delete = io.sqm.core.MergeDeleteAction.of();
        MergeAction insert = io.sqm.core.MergeInsertAction.of(java.util.List.of(id("id")), row(lit(1)));

        assertEquals("UPDATE", Match.<String>mergeAction(update).update(i -> "UPDATE").otherwise(ignored -> "OTHER"));
        assertEquals("DELETE", Match.<String>mergeAction(delete).delete(i -> "DELETE").otherwise(ignored -> "OTHER"));
        assertEquals("INSERT", Match.<String>mergeAction(insert).insert(i -> "INSERT").otherwise(ignored -> "OTHER"));
    }
}
