package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class DeleteStatementTest {

    @Test
    void builderCreatesImmutableDeleteStatement() {
        var statement = delete(tbl("users"))
            .hint("BKA", "users")
            .using(tbl("users"))
            .join(inner(tbl("orders").as("o")).on(col("users", "id").eq(col("o", "user_id"))))
            .where(col("id").eq(lit(1)))
            .build();

        assertEquals("users", statement.table().name().value());
        assertEquals(col("id").eq(lit(1)), statement.where());
        assertEquals(List.of("BKA"), statement.hints().stream().map(h -> h.name().value()).toList());
        assertEquals(1, statement.using().size());
        assertEquals(1, statement.joins().size());
        assertThrows(UnsupportedOperationException.class, () -> statement.using().add(tbl("x")));
        assertThrows(UnsupportedOperationException.class, () -> statement.joins().add(inner(tbl("x")).on(col("x", "id").eq(col("users", "id")))));
        assertThrows(UnsupportedOperationException.class, () -> statement.hints().add(statementHint("NO_ICP", "users")));
    }

    @Test
    void supportsCanonicalFactoryAndBuilderMutator() {
        var statement = DeleteStatement.of(tbl("users"), List.of(), List.of(), null, null, List.of());
        assertNull(statement.where());
        assertTrue(statement.using().isEmpty());
        assertTrue(statement.joins().isEmpty());

        var built = DeleteStatement.builder(tbl("users"))
            .table(tbl("accounts"))
            .hints(List.of(statementHint("MAX_EXECUTION_TIME", 1000)))
            .using(tbl("src"))
            .joins(inner(tbl("audit")).on(col("src", "id").eq(col("audit", "account_id"))))
            .build();
        assertEquals("accounts", built.table().name().value());
        assertEquals(List.of("MAX_EXECUTION_TIME"), built.hints().stream().map(h -> h.name().value()).toList());
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
            result(col("users", "id")),
            List.of());

        assertEquals(1, statement.using().size());
        assertEquals(1, statement.joins().size());
        assertEquals(1, statement.result().items().size());

        var withoutReturning = DeleteStatement.of(
            tbl("users"),
            List.of(tbl("users")),
            List.of(inner(tbl("orders")).on(col("users", "id").eq(col("orders", "user_id")))),
            col("users", "id").eq(lit(1)),
            null,
            List.of());

        assertNull(withoutReturning.result());
    }

    @Test
    void normalizesNullUsingJoinsAndReturningInFactory() {
        var statement = DeleteStatement.of(tbl("users"), null, null, null, null, null);
        assertTrue(statement.using().isEmpty());
        assertTrue(statement.joins().isEmpty());
        assertNull(statement.result());
        assertTrue(statement.hints().isEmpty());
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
        assertThrows(NullPointerException.class, () -> DeleteStatement.of(null, List.of(), List.of(), null, null, List.of()));
        assertThrows(NullPointerException.class, () -> DeleteStatement.builder(tbl("users")).table(null));
        assertThrows(NullPointerException.class, () -> DeleteStatement.builder(tbl("users")).using((TableRef[]) null));
        assertThrows(NullPointerException.class, () -> DeleteStatement.builder(tbl("users")).using((List<TableRef>) null));
        assertThrows(NullPointerException.class, () -> DeleteStatement.builder(tbl("users")).joins((Join[]) null));
        assertThrows(NullPointerException.class, () -> DeleteStatement.builder(tbl("users")).join(null));
        assertThrows(NullPointerException.class, () -> DeleteStatement.builder(tbl("users")).result((ResultItem[]) null));
        assertThrows(NullPointerException.class, () -> DeleteStatement.builder(tbl("users")).result((List<ResultItem>) null));
        assertThrows(NullPointerException.class, () -> DeleteStatement.builder(tbl("users")).hints(null));
        assertThrows(NullPointerException.class, () -> DeleteStatement.builder(tbl("users")).hint(null));
    }

    @Test
    void builderCanCopyExistingStatement() {
        var original = delete(tbl("users"))
            .hint("BKA", "users")
            .result(deleted("id"))
            .using(tbl("source_users"))
            .build();

        var copied = DeleteStatement.builder(original)
            .clearHints()
            .build();

        assertEquals(List.of("BKA"), original.hints().stream().map(h -> h.name().value()).toList());
        assertTrue(copied.hints().isEmpty());
        assertEquals(original.result(), copied.result());
        assertEquals(original.using(), copied.using());
        assertEquals(original.table(), copied.table());
    }

    @Test
    void supportsTypedStatementHints() {
        var statement = delete(tbl("users"))
            .hint(statementHint("MAX_EXECUTION_TIME", 1000))
            .hint("BKA", "users")
            .build();

        assertEquals(2, statement.hints().size());
        assertEquals("MAX_EXECUTION_TIME", statement.hints().getFirst().name().value());
        assertEquals("users",
            assertInstanceOf(IdentifierHintArg.class, statement.hints().getLast().args().getFirst()).value().value());
    }
}
