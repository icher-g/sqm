package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.set;
import static io.sqm.dsl.Dsl.tbl;
import static io.sqm.dsl.Dsl.update;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateStatementTest {

    @Test
    void builderCreatesImmutableUpdateStatement() {
        var statement = update(tbl("users"))
            .set(set("name", lit("alice")))
            .where(col("id").eq(lit(1)))
            .build();

        assertEquals("users", statement.table().name().value());
        assertEquals(1, statement.assignments().size());
        assertEquals("name", statement.assignments().getFirst().column().value());
        assertThrows(UnsupportedOperationException.class, () -> statement.assignments().add(set("x", lit(1))));
    }

    @Test
    void supportsOfOverloadAndBuilderConvenienceMethods() {
        var statement = UpdateStatement.of(tbl("users"), List.of(set("name", lit("alice"))));
        assertNull(statement.where());

        var built = UpdateStatement.builder(tbl("users"))
            .set(Identifier.of("name"), lit("alice"))
            .build();
        assertEquals(1, built.assignments().size());
    }

    @Test
    void equalityAndHashDependOnShape() {
        var first = update(tbl("users"))
            .set(set("name", lit("alice")))
            .where(col("id").eq(lit(1)))
            .build();
        var second = update(tbl("users"))
            .set(set("name", lit("alice")))
            .where(col("id").eq(lit(1)))
            .build();
        var third = update(tbl("users"))
            .set(set("name", lit("bob")))
            .build();

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, third);
        assertNull(third.where());
    }

    @Test
    void validatesRequiredMembers() {
        assertThrows(NullPointerException.class, () -> UpdateStatement.of(null, List.of(set("name", lit("alice"))), null));
        assertThrows(NullPointerException.class, () -> UpdateStatement.of(tbl("users"), null, null));
        assertThrows(IllegalArgumentException.class, () -> UpdateStatement.of(tbl("users"), List.of(), null));
        assertThrows(IllegalStateException.class, () -> update(tbl("users")).build());
        assertThrows(NullPointerException.class, () -> UpdateStatement.builder(tbl("users")).table(null));
        assertThrows(NullPointerException.class, () -> UpdateStatement.builder(tbl("users")).assignments(null));
        assertThrows(NullPointerException.class, () -> UpdateStatement.builder(tbl("users")).set(null));
    }
}
