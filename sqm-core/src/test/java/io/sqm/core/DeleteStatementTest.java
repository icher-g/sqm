package io.sqm.core;

import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.delete;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeleteStatementTest {

    @Test
    void builderCreatesImmutableDeleteStatement() {
        var statement = delete(tbl("users"))
            .where(col("id").eq(lit(1)))
            .build();

        assertEquals("users", statement.table().name().value());
        assertEquals(col("id").eq(lit(1)), statement.where());
    }

    @Test
    void equalityAndHashDependOnShape() {
        var first = delete(tbl("users"))
            .where(col("id").eq(lit(1)))
            .build();
        var second = delete(tbl("users"))
            .where(col("id").eq(lit(1)))
            .build();
        var third = delete(tbl("users"))
            .build();

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, third);
        assertNull(third.where());
    }

    @Test
    void validatesRequiredMembers() {
        assertThrows(NullPointerException.class, () -> DeleteStatement.of(null, null));
        assertThrows(NullPointerException.class, () -> DeleteStatement.of(null));
    }
}
