package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.inner;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.set;
import static io.sqm.dsl.Dsl.tbl;
import static io.sqm.dsl.Dsl.update;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateStatementTest {

    @Test
    void builderCreatesImmutableUpdateStatement() {
        var statement = update(tbl("users"))
            .join(inner(tbl("orders").as("o")).on(col("users", "id").eq(col("o", "user_id"))))
            .set(set("name", lit("alice")))
            .from(tbl("source_users"))
            .where(col("id").eq(lit(1)))
            .build();

        assertEquals("users", statement.table().name().value());
        assertEquals(1, statement.assignments().size());
        assertEquals(List.of("name"), statement.assignments().getFirst().column().values());
        assertEquals(1, statement.joins().size());
        assertEquals(1, statement.from().size());
        assertThrows(UnsupportedOperationException.class, () -> statement.assignments().add(set("x", lit(1))));
        assertThrows(UnsupportedOperationException.class, () -> statement.joins().add(inner(tbl("x")).on(col("x", "id").eq(col("users", "id")))));
        assertThrows(UnsupportedOperationException.class, () -> statement.from().add(tbl("x")));
    }

    @Test
    void supportsOfOverloadAndBuilderConvenienceMethods() {
        var statement = UpdateStatement.of(
            tbl("users"),
            List.of(set("name", lit("alice"))),
            List.of(inner(tbl("orders")).on(col("users", "id").eq(col("orders", "user_id")))));
        assertNull(statement.where());
        assertTrue(statement.from().isEmpty());
        assertEquals(1, statement.joins().size());

        var built = UpdateStatement.builder(tbl("users"))
            .set(QualifiedName.of("u", "name"), lit("alice"))
            .joins(inner(tbl("orders")).on(col("users", "id").eq(col("orders", "user_id"))))
            .from(tbl("src"))
            .build();
        assertEquals(1, built.assignments().size());
        assertEquals(List.of("u", "name"), built.assignments().getFirst().column().values());
        assertEquals(1, built.joins().size());
        assertEquals(1, built.from().size());
    }

    @Test
    void supportsFactoryOverloadsWithFromAndReturning() {
        var statement = UpdateStatement.of(
            tbl("users"),
            List.of(set("name", lit("alice"))),
            List.of(inner(tbl("orders")).on(col("users", "id").eq(col("orders", "user_id")))),
            List.of(tbl("src_users")),
            col("users", "id").eq(lit(1)),
            List.of(io.sqm.core.ExprSelectItem.of(col("users", "id"), null)));

        assertEquals(1, statement.joins().size());
        assertEquals(1, statement.from().size());
        assertEquals(1, statement.returning().size());

        var withoutReturning = UpdateStatement.of(
            tbl("users"),
            List.of(set("name", lit("alice"))),
            List.of(inner(tbl("orders")).on(col("users", "id").eq(col("orders", "user_id")))),
            List.of(tbl("src_users")),
            col("users", "id").eq(lit(1)));

        assertTrue(withoutReturning.returning().isEmpty());
    }

    @Test
    void normalizesNullFromJoinsAndReturningInFactory() {
        var statement = UpdateStatement.of(tbl("users"), List.of(set("name", lit("alice"))), null, null, null, null);
        assertTrue(statement.joins().isEmpty());
        assertTrue(statement.from().isEmpty());
        assertTrue(statement.returning().isEmpty());
    }

    @Test
    void equalityAndHashDependOnShape() {
        var first = update(tbl("users"))
            .join(inner(tbl("orders")).on(col("users", "id").eq(col("orders", "user_id"))))
            .set(set("name", lit("alice")))
            .from(tbl("source_users"))
            .where(col("id").eq(lit(1)))
            .build();
        var second = update(tbl("users"))
            .join(inner(tbl("orders")).on(col("users", "id").eq(col("orders", "user_id"))))
            .set(set("name", lit("alice")))
            .from(tbl("source_users"))
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
        assertThrows(NullPointerException.class, () -> UpdateStatement.of(null, List.of(set("name", lit("alice"))), (Predicate) null));
        assertThrows(NullPointerException.class, () -> UpdateStatement.of(tbl("users"), null, (Predicate) null));
        assertThrows(IllegalArgumentException.class, () -> UpdateStatement.of(tbl("users"), List.of(), (Predicate) null));
        assertThrows(IllegalStateException.class, () -> update(tbl("users")).build());
        assertThrows(NullPointerException.class, () -> UpdateStatement.builder(tbl("users")).table(null));
        assertThrows(NullPointerException.class, () -> UpdateStatement.builder(tbl("users")).assignments(null));
        assertThrows(NullPointerException.class, () -> UpdateStatement.builder(tbl("users")).joins((Join[]) null));
        assertThrows(NullPointerException.class, () -> UpdateStatement.builder(tbl("users")).join(null));
        assertThrows(NullPointerException.class, () -> UpdateStatement.builder(tbl("users")).from((TableRef[]) null));
        assertThrows(NullPointerException.class, () -> UpdateStatement.builder(tbl("users")).returning((SelectItem[]) null));
        assertThrows(NullPointerException.class, () -> UpdateStatement.builder(tbl("users")).returning((List<SelectItem>) null));
        assertThrows(NullPointerException.class, () -> UpdateStatement.builder(tbl("users")).set(null));
    }
}