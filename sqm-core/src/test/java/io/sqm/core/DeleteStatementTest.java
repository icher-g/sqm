package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.deleted;
import static io.sqm.dsl.Dsl.delete;
import static io.sqm.dsl.Dsl.inner;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.output;
import static io.sqm.dsl.Dsl.outputItem;
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
            .optimizerHint("BKA(users)")
            .using(tbl("users"))
            .join(inner(tbl("orders").as("o")).on(col("users", "id").eq(col("o", "user_id"))))
            .where(col("id").eq(lit(1)))
            .build();

        assertEquals("users", statement.table().name().value());
        assertEquals(col("id").eq(lit(1)), statement.where());
        assertEquals(List.of("BKA(users)"), statement.optimizerHints());
        assertEquals(1, statement.using().size());
        assertEquals(1, statement.joins().size());
        assertThrows(UnsupportedOperationException.class, () -> statement.using().add(tbl("x")));
        assertThrows(UnsupportedOperationException.class, () -> statement.joins().add(inner(tbl("x")).on(col("x", "id").eq(col("users", "id")))));
        assertThrows(UnsupportedOperationException.class, () -> statement.optimizerHints().add("NO_ICP(users)"));
    }

    @Test
    void supportsCanonicalFactoryAndBuilderMutator() {
        var statement = DeleteStatement.of(tbl("users"), List.of(), List.of(), null, null, List.of(), List.of());
        assertNull(statement.where());
        assertTrue(statement.using().isEmpty());
        assertTrue(statement.joins().isEmpty());

        var built = DeleteStatement.builder(tbl("users"))
            .table(tbl("accounts"))
            .optimizerHints(List.of("MAX_EXECUTION_TIME(1000)"))
            .using(tbl("src"))
            .joins(inner(tbl("audit")).on(col("src", "id").eq(col("audit", "account_id"))))
            .build();
        assertEquals("accounts", built.table().name().value());
        assertEquals(List.of("MAX_EXECUTION_TIME(1000)"), built.optimizerHints());
        assertEquals(1, built.using().size());
        assertEquals(1, built.joins().size());
    }

    @Test
    void supportsCanonicalFactoryWithUsingJoinsAndReturning() {
        var statement = DeleteStatement.of(
            tbl("users"),
            List.of(tbl("users")),
            List.of(inner(tbl("orders")).on(col("users", "id").eq(col("orders", "user_id")))),
            col("users", "id").eq(lit(1)),
            null,
            List.of(io.sqm.core.ExprSelectItem.of(col("users", "id"), null)),
            List.of());

        assertEquals(1, statement.using().size());
        assertEquals(1, statement.joins().size());
        assertEquals(1, statement.returning().size());

        var withoutReturning = DeleteStatement.of(
            tbl("users"),
            List.of(tbl("users")),
            List.of(inner(tbl("orders")).on(col("users", "id").eq(col("orders", "user_id")))),
            col("users", "id").eq(lit(1)),
            null,
            List.of(),
            List.of());

        assertTrue(withoutReturning.returning().isEmpty());
    }

    @Test
    void normalizesNullUsingJoinsAndReturningInFactory() {
        var statement = DeleteStatement.of(tbl("users"), null, null, null, null, null, null);
        assertTrue(statement.using().isEmpty());
        assertTrue(statement.joins().isEmpty());
        assertTrue(statement.returning().isEmpty());
        assertTrue(statement.optimizerHints().isEmpty());
    }

    @Test
    void equalityAndHashDependOnShape() {
        var first = delete(tbl("users"))
            .using(tbl("users"))
            .join(inner(tbl("orders")).on(col("users", "id").eq(col("orders", "user_id"))))
            .where(col("id").eq(lit(1)))
            .build();
        var second = delete(tbl("users"))
            .using(tbl("users"))
            .join(inner(tbl("orders")).on(col("users", "id").eq(col("orders", "user_id"))))
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
        assertThrows(NullPointerException.class, () -> DeleteStatement.of(null, List.of(), List.of(), null, null, List.of(), List.of()));
        assertThrows(NullPointerException.class, () -> DeleteStatement.builder(tbl("users")).table(null));
        assertThrows(NullPointerException.class, () -> DeleteStatement.builder(tbl("users")).using((TableRef[]) null));
        assertThrows(NullPointerException.class, () -> DeleteStatement.builder(tbl("users")).using((List<TableRef>) null));
        assertThrows(NullPointerException.class, () -> DeleteStatement.builder(tbl("users")).joins((Join[]) null));
        assertThrows(NullPointerException.class, () -> DeleteStatement.builder(tbl("users")).join(null));
        assertThrows(NullPointerException.class, () -> DeleteStatement.builder(tbl("users")).returning((SelectItem[]) null));
        assertThrows(NullPointerException.class, () -> DeleteStatement.builder(tbl("users")).returning((List<SelectItem>) null));
        assertThrows(NullPointerException.class, () -> DeleteStatement.builder(tbl("users")).optimizerHints(null));
        assertThrows(NullPointerException.class, () -> DeleteStatement.builder(tbl("users")).optimizerHint(null));
    }

    @Test
    void builderCanCopyExistingStatement() {
        var original = delete(tbl("users"))
            .optimizerHint("BKA(users)")
            .output(output(outputItem(deleted("id"))))
            .using(tbl("source_users"))
            .build();

        var copied = DeleteStatement.builder(original)
            .clearOptimizerHints()
            .build();

        assertEquals(List.of("BKA(users)"), original.optimizerHints());
        assertTrue(copied.optimizerHints().isEmpty());
        assertEquals(original.output(), copied.output());
        assertEquals(original.using(), copied.using());
        assertEquals(original.table(), copied.table());
    }
}
