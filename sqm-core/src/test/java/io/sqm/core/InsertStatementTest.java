package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class InsertStatementTest {

    @Test
    void builderCreatesImmutableInsertStatement() {
        var statement = insert(tbl("users"))
            .ignore()
            .columns(Identifier.of("id"), Identifier.of("name"))
            .values(rows(
                row(lit(1), lit("alice")),
                row(lit(2), lit("bob"))))
            .onConflictDoUpdate(List.of(Identifier.of("id")), List.of(set("name", lit("alice2"))), col("id").eq(lit(1)))
            .result(col("id"))
            .build();

        assertEquals(InsertStatement.InsertMode.IGNORE, statement.insertMode());
        assertEquals("users", statement.table().name().value());
        assertEquals(2, statement.columns().size());
        assertInstanceOf(RowListExpr.class, statement.source());
        assertEquals(InsertStatement.OnConflictAction.DO_UPDATE, statement.onConflictAction());
        assertEquals(1, statement.conflictUpdateAssignments().size());
        assertEquals(1, statement.result().items().size());
        assertThrows(UnsupportedOperationException.class, () -> statement.columns().add(Identifier.of("x")));
        assertThrows(UnsupportedOperationException.class, () -> statement.conflictTarget().add(Identifier.of("x")));
        assertThrows(UnsupportedOperationException.class, () -> statement.conflictUpdateAssignments().add(set("x", lit(1))));
        assertThrows(UnsupportedOperationException.class, () -> statement.result().items().add(col("name").toResultItem()));
    }

    @Test
    void supportsCanonicalFactoryAndBuilderConvenienceMethods() {
        var source = row(lit(1));
        var ofStatement = InsertStatement.of(
            InsertStatement.InsertMode.REPLACE,
            tbl("users"),
            List.of(),
            source,
            List.of(),
            InsertStatement.OnConflictAction.NONE,
            List.of(),
            null,
            null);

        assertEquals(InsertStatement.InsertMode.REPLACE, ofStatement.insertMode());
        assertEquals(0, ofStatement.columns().size());
        assertInstanceOf(RowExpr.class, ofStatement.source());

        var built = InsertStatement.builder(tbl("users"))
            .columns(Identifier.of("id"))
            .query(io.sqm.dsl.Dsl.select(lit(1)).build())
            .onConflictDoNothing(Identifier.of("id"))
            .result(col("id").toSelectItem())
            .build();

        assertEquals(InsertStatement.InsertMode.STANDARD, built.insertMode());
        assertEquals(1, built.columns().size());
        assertEquals(InsertStatement.OnConflictAction.DO_NOTHING, built.onConflictAction());
        assertEquals(1, built.result().items().size());
        assertInstanceOf(io.sqm.core.SelectQuery.class, built.source());
    }

    @Test
    void builderSupportsListAndVarargConvenienceOverloads() {
        var statement = InsertStatement.builder(tbl("users"))
            .replace()
            .standard()
            .columns(List.of(Identifier.of("id"), Identifier.of("name")))
            .values(row(lit(1), lit("alice")))
            .onConflictDoUpdate(set("name", lit("alice2")))
            .onConflictDoNothing()
            .result(inserted("id"))
            .build();

        assertEquals(InsertStatement.InsertMode.STANDARD, statement.insertMode());
        assertEquals(2, statement.columns().size());
        assertEquals(InsertStatement.OnConflictAction.DO_NOTHING, statement.onConflictAction());
        assertTrue(statement.conflictTarget().isEmpty());
        assertTrue(statement.conflictUpdateAssignments().isEmpty());
        assertNull(statement.conflictUpdateWhere());
        assertEquals(OutputRowSource.INSERTED, statement.result().items().getFirst().matchResultItem().expr(e -> e.expr().matchExpression().outputColumn(OutputColumnExpr::source).orElse(null)).orElse(null));
    }

    @Test
    void canonicalFactorySupportsStandardModeWithoutColumns() {
        var statementWithColumns = InsertStatement.of(
            InsertStatement.InsertMode.STANDARD,
            tbl("users"),
            List.of(Identifier.of("id")),
            row(lit(1)),
            List.of(),
            InsertStatement.OnConflictAction.NONE,
            List.of(),
            null,
            null);
        var statementWithoutColumns = InsertStatement.of(
            InsertStatement.InsertMode.STANDARD,
            tbl("users"),
            List.of(),
            row(lit(1)),
            List.of(),
            InsertStatement.OnConflictAction.NONE,
            List.of(),
            null,
            null);

        assertEquals(InsertStatement.InsertMode.STANDARD, statementWithColumns.insertMode());
        assertEquals(1, statementWithColumns.columns().size());
        assertEquals(InsertStatement.InsertMode.STANDARD, statementWithoutColumns.insertMode());
        assertTrue(statementWithoutColumns.columns().isEmpty());
    }

    @Test
    void normalizesNullCollectionsInFactories() {
        var statement = InsertStatement.of(InsertStatement.InsertMode.IGNORE, tbl("users"), null, row(lit(1)), null, null, null, null, null);
        var statementWithReturningOverload = InsertStatement.of(
            InsertStatement.InsertMode.REPLACE,
            tbl("users"),
            List.of(),
            row(lit(1)),
            List.of(),
            InsertStatement.OnConflictAction.NONE,
            List.of(),
            null,
            null);

        assertEquals(InsertStatement.InsertMode.IGNORE, statement.insertMode());
        assertTrue(statement.columns().isEmpty());
        assertTrue(statement.conflictTarget().isEmpty());
        assertEquals(InsertStatement.OnConflictAction.NONE, statement.onConflictAction());
        assertTrue(statement.conflictUpdateAssignments().isEmpty());
        assertEquals(InsertStatement.InsertMode.REPLACE, statementWithReturningOverload.insertMode());
        assertTrue(statementWithReturningOverload.columns().isEmpty());
    }

    @Test
    void equalityAndHashDependOnShape() {
        var first = insert(tbl("users"))
            .ignore()
            .columns(Identifier.of("id"))
            .values(row(lit(1)))
            .onConflictDoNothing(Identifier.of("id"))
            .result(col("id").toSelectItem())
            .build();
        var second = insert(tbl("users"))
            .ignore()
            .columns(Identifier.of("id"))
            .values(row(lit(1)))
            .onConflictDoNothing(Identifier.of("id"))
            .result(col("id").toSelectItem())
            .build();
        var third = insert(tbl("users"))
            .columns(Identifier.of("id"))
            .values(row(lit(1)))
            .onConflictDoNothing(Identifier.of("id"))
            .result(col("id").toSelectItem())
            .build();

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, third);
    }

    @Test
    void validatesRequiredMembers() {
        var defaultModeStatement = InsertStatement.of(null, tbl("users"), List.of(), row(lit(1)), List.of(), null, List.of(), null, null);
        assertEquals(InsertStatement.InsertMode.STANDARD, defaultModeStatement.insertMode());
        assertThrows(NullPointerException.class, () -> InsertStatement.of(InsertStatement.InsertMode.STANDARD, null, List.of(), row(lit(1)), List.of(), InsertStatement.OnConflictAction.NONE, List.of(), null, null));
        assertThrows(NullPointerException.class, () -> InsertStatement.of(InsertStatement.InsertMode.STANDARD, tbl("users"), List.of(), null, List.of(), InsertStatement.OnConflictAction.NONE, List.of(), null, null));
        assertThrows(IllegalStateException.class, () -> insert(tbl("users")).build());
        assertThrows(NullPointerException.class, () -> InsertStatement.builder(tbl("users")).table(null));
        assertThrows(NullPointerException.class, () -> InsertStatement.builder(tbl("users")).insertMode(null));
        assertThrows(NullPointerException.class, () -> InsertStatement.builder(tbl("users")).columns((Identifier[]) null));
        assertThrows(NullPointerException.class, () -> InsertStatement.builder(tbl("users")).source(null));
        assertThrows(NullPointerException.class, () -> InsertStatement.builder(tbl("users")).result((SelectItem[]) null));
        assertThrows(IllegalArgumentException.class, () -> InsertStatement.of(InsertStatement.InsertMode.STANDARD, tbl("users"), List.of(), row(lit(1)), List.of(), InsertStatement.OnConflictAction.DO_UPDATE, List.of(), null, null));
        assertThrows(IllegalArgumentException.class, () -> InsertStatement.of(InsertStatement.InsertMode.STANDARD, tbl("users"), List.of(), row(lit(1)), List.of(Identifier.of("id")), InsertStatement.OnConflictAction.NONE, List.of(), null, null));
        assertThrows(IllegalArgumentException.class, () -> InsertStatement.of(InsertStatement.InsertMode.STANDARD, tbl("users"), List.of(), row(lit(1)), List.of(), InsertStatement.OnConflictAction.DO_NOTHING, List.of(set("id", lit(2))), null, null));
    }
}
