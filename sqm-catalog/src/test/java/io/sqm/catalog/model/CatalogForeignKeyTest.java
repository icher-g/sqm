package io.sqm.catalog.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CatalogForeignKeyTest {
    @Test
    void constructor_defensively_copies_lists() {
        var source = new ArrayList<>(List.of("user_id"));
        var target = new ArrayList<>(List.of("id"));

        var fk = CatalogForeignKey.of("fk_orders_users", source, "public", "users", target);
        source.add("other");
        target.add("other");

        assertEquals(List.of("user_id"), fk.sourceColumns());
        assertEquals(List.of("id"), fk.targetColumns());
    }

    @Test
    void constructor_rejects_required_nulls() {
        assertThrows(NullPointerException.class, () -> new CatalogForeignKey("fk", null, "public", "users", List.of("id")));
        assertThrows(NullPointerException.class, () -> new CatalogForeignKey("fk", List.of("user_id"), "public", null, List.of("id")));
        assertThrows(NullPointerException.class, () -> new CatalogForeignKey("fk", List.of("user_id"), "public", "users", null));
    }
}

