package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.delete;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeleteStatementTest {

    @Test
    void builderCreatesImmutableDeleteStatement() {
        var statement = delete(tbl("users"))
            .using(tbl("source_users"))
            .where(col("id").eq(lit(1)))
            .build();

        assertEquals("users", statement.table().name().value());
        assertEquals(col("id").eq(lit(1)), statement.where());
        assertEquals(1, statement.using().size());
        assertThrows(UnsupportedOperationException.class, () -> statement.using().add(tbl("x")));
    }

    @Test
    void supportsOfOverloadAndBuilderMutator() {
        var statement = DeleteStatement.of(tbl("users"));
        assertNull(statement.where());
        assertTrue(statement.using().isEmpty());

        var built = DeleteStatement.builder(tbl("users"))
            .table(tbl("accounts"))
            .using(tbl("src"))
            .build();
        assertEquals("accounts", built.table().name().value());
        assertEquals(1, built.using().size());
    }

    @Test
    void normalizesNullUsingInFactory() {
        var statement = DeleteStatement.of(tbl("users"), null, null);
        assertTrue(statement.using().isEmpty());
    }

    @Test
    void equalityAndHashDependOnShape() {
        var first = delete(tbl("users"))
            .using(tbl("source_users"))
            .where(col("id").eq(lit(1)))
            .build();
        var second = delete(tbl("users"))
            .using(tbl("source_users"))
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
        assertThrows(NullPointerException.class, () -> DeleteStatement.builder(tbl("users")).table(null));
        assertThrows(NullPointerException.class, () -> DeleteStatement.builder(tbl("users")).using((TableRef[]) null));
        assertThrows(NullPointerException.class, () -> DeleteStatement.builder(tbl("users")).using((List<TableRef>) null));
    }
}
