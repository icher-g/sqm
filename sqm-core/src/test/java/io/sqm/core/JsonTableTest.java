package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class JsonTableTest {
    @Test
    void jsonPathRejectsBlankText() {
        assertThrows(IllegalArgumentException.class, () -> jsonPath(" "));
    }

    @Test
    void jsonTableRequiresAtLeastOneColumnAndCopiesColumns() {
        assertThrows(IllegalArgumentException.class, () -> JsonTable.of(col("payload"), jsonPath("$"), java.util.List.of()));

        var columns = new ArrayList<JsonTableColumn>();
        columns.add(jsonScalar("id", type("NUMBER"), jsonPath("$.id")));
        var table = JsonTable.of(col("payload"), jsonPath("$"), columns);

        columns.add(jsonOrdinality("ord"));

        assertEquals(1, table.columns().size());
        assertNull(table.alias());
        assertEquals("jt", table.as("jt").alias().value());
        assertNull(table.as((String) null).alias());
    }

    @Test
    void nestedPathRequiresColumnsCopiesColumnsAndHasNoDirectName() {
        assertThrows(IllegalArgumentException.class, () -> jsonNested(jsonPath("$.children[*]")));

        var columns = new ArrayList<JsonTableColumn>();
        columns.add(jsonScalar("child_id", type("NUMBER"), jsonPath("$.id")));
        var nested = JsonTableNestedPathColumn.of(jsonPath("$.children[*]"), columns);

        columns.add(jsonOrdinality("ord"));

        assertNull(nested.name());
        assertEquals(1, nested.columns().size());
    }

    @Test
    void behaviorGuardsDefaultExpressionRules() {
        assertEquals(JsonTableBehavior.Kind.NULL, jsonBehavior(JsonTableBehavior.Kind.NULL).kind());
        assertEquals("fallback", jsonBehavior(JsonTableBehavior.Kind.DEFAULT, lit("fallback"))
            .defaultExpression()
            .matchExpression()
            .literal(l -> l.value().toString())
            .orElse("missing"));

        assertThrows(IllegalArgumentException.class, () -> jsonBehavior(JsonTableBehavior.Kind.DEFAULT));
        assertThrows(IllegalArgumentException.class, () -> jsonBehavior(JsonTableBehavior.Kind.ERROR, lit("x")));
    }

    @Test
    void existsColumnDefaultsAndExplicitBehavior() {
        var defaulted = jsonExists("present", type("BOOLEAN"), jsonPath("$.present"));
        var explicit = jsonExists(id("present"), type("BOOLEAN"), jsonPath("$.present"), jsonBehavior(JsonTableBehavior.Kind.ERROR));

        assertNull(defaulted.onError());
        assertEquals("present", explicit.name().value());
        assertEquals(JsonTableBehavior.Kind.ERROR, explicit.onError().kind());
    }
}
