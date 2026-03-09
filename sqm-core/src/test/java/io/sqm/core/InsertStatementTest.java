package io.sqm.core;

import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.insert;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.rows;
import static io.sqm.dsl.Dsl.set;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InsertStatementTest {

    @Test
    void builderCreatesImmutableInsertStatement() {
        var statement = insert(tbl("users"))
            .columns(Identifier.of("id"), Identifier.of("name"))
            .values(rows(
                row(lit(1), lit("alice")),
                row(lit(2), lit("bob"))))
            .onConflictDoUpdate(java.util.List.of(Identifier.of("id")), java.util.List.of(set("name", lit("alice2"))), col("id").eq(lit(1)))
            .returning(col("id").toSelectItem())
            .build();

        assertEquals("users", statement.table().name().value());
        assertEquals(2, statement.columns().size());
        assertInstanceOf(RowListExpr.class, statement.source());
        assertEquals(InsertStatement.OnConflictAction.DO_UPDATE, statement.onConflictAction());
        assertEquals(1, statement.conflictUpdateAssignments().size());
        assertEquals(1, statement.returning().size());
        assertThrows(UnsupportedOperationException.class, () -> statement.columns().add(Identifier.of("x")));
        assertThrows(UnsupportedOperationException.class, () -> statement.conflictTarget().add(Identifier.of("x")));
        assertThrows(UnsupportedOperationException.class, () -> statement.conflictUpdateAssignments().add(set("x", lit(1))));
        assertThrows(UnsupportedOperationException.class, () -> statement.returning().add(col("name").toSelectItem()));
    }

    @Test
    void supportsOfOverloadAndBuilderConvenienceMethods() {
        var source = row(lit(1));
        var ofStatement = InsertStatement.of(tbl("users"), source);

        assertEquals(0, ofStatement.columns().size());
        assertInstanceOf(RowExpr.class, ofStatement.source());

        var built = InsertStatement.builder(tbl("users"))
            .columns(Identifier.of("id"))
            .query(io.sqm.dsl.Dsl.select(lit(1)).build())
            .onConflictDoNothing(Identifier.of("id"))
            .returning(col("id").toSelectItem())
            .build();

        assertEquals(1, built.columns().size());
        assertEquals(InsertStatement.OnConflictAction.DO_NOTHING, built.onConflictAction());
        assertEquals(1, built.returning().size());
        assertInstanceOf(io.sqm.core.SelectQuery.class, built.source());
    }

    @Test
    void normalizesNullCollectionsInFactories() {
        var statement = InsertStatement.of(tbl("users"), null, row(lit(1)), null, null, null, null, null);
        var statementWithReturningOverload = InsertStatement.of(tbl("users"), row(lit(1)), null);

        assertTrue(statement.columns().isEmpty());
        assertTrue(statement.conflictTarget().isEmpty());
        assertEquals(InsertStatement.OnConflictAction.NONE, statement.onConflictAction());
        assertTrue(statement.conflictUpdateAssignments().isEmpty());
        assertTrue(statement.returning().isEmpty());
        assertTrue(statementWithReturningOverload.columns().isEmpty());
        assertTrue(statementWithReturningOverload.returning().isEmpty());
    }

    @Test
    void equalityAndHashDependOnShape() {
        var first = insert(tbl("users"))
            .columns(Identifier.of("id"))
            .values(row(lit(1)))
            .onConflictDoNothing(Identifier.of("id"))
            .returning(col("id").toSelectItem())
            .build();
        var second = insert(tbl("users"))
            .columns(Identifier.of("id"))
            .values(row(lit(1)))
            .onConflictDoNothing(Identifier.of("id"))
            .returning(col("id").toSelectItem())
            .build();
        var third = insert(tbl("users"))
            .columns(Identifier.of("id"))
            .query(io.sqm.dsl.Dsl.select(lit(1)).build())
            .build();

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, third);
    }

    @Test
    void validatesRequiredMembers() {
        assertThrows(NullPointerException.class, () -> InsertStatement.of(null, java.util.List.of(), row(lit(1))));
        assertThrows(NullPointerException.class, () -> InsertStatement.of(tbl("users"), java.util.List.of(), null));
        assertThrows(IllegalStateException.class, () -> insert(tbl("users")).build());
        assertThrows(NullPointerException.class, () -> InsertStatement.builder(tbl("users")).table(null));
        assertThrows(NullPointerException.class, () -> InsertStatement.builder(tbl("users")).columns((Identifier[]) null));
        assertThrows(NullPointerException.class, () -> InsertStatement.builder(tbl("users")).source(null));
        assertThrows(NullPointerException.class, () -> InsertStatement.builder(tbl("users")).returning((SelectItem[]) null));
        assertThrows(IllegalArgumentException.class, () -> InsertStatement.of(tbl("users"), java.util.List.of(), row(lit(1)), java.util.List.of(), InsertStatement.OnConflictAction.DO_UPDATE, java.util.List.of(), null, java.util.List.of()));
        assertThrows(IllegalArgumentException.class, () -> InsertStatement.of(tbl("users"), java.util.List.of(), row(lit(1)), java.util.List.of(Identifier.of("id")), InsertStatement.OnConflictAction.NONE, java.util.List.of(), null, java.util.List.of()));
    }
}
