package io.sqm.core;

import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.insert;
import static io.sqm.dsl.Dsl.inserted;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.output;
import static io.sqm.dsl.Dsl.outputItem;
import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.rows;
import static io.sqm.dsl.Dsl.set;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InsertStatementTest {

    @Test
    void builderCreatesImmutableInsertStatement() {
        var statement = insert(tbl("users"))
            .ignore()
            .columns(Identifier.of("id"), Identifier.of("name"))
            .values(rows(
                row(lit(1), lit("alice")),
                row(lit(2), lit("bob"))))
            .onConflictDoUpdate(java.util.List.of(Identifier.of("id")), java.util.List.of(set("name", lit("alice2"))), col("id").eq(lit(1)))
            .returning(col("id").toSelectItem())
            .build();

        assertEquals(InsertStatement.InsertMode.IGNORE, statement.insertMode());
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
    void supportsCanonicalFactoryAndBuilderConvenienceMethods() {
        var source = row(lit(1));
        var ofStatement = InsertStatement.of(
            InsertStatement.InsertMode.REPLACE,
            tbl("users"),
            java.util.List.of(),
            source,
            java.util.List.of(),
            InsertStatement.OnConflictAction.NONE,
            java.util.List.of(),
            null,
            null,
            java.util.List.of());

        assertEquals(InsertStatement.InsertMode.REPLACE, ofStatement.insertMode());
        assertEquals(0, ofStatement.columns().size());
        assertInstanceOf(RowExpr.class, ofStatement.source());

        var built = InsertStatement.builder(tbl("users"))
            .columns(Identifier.of("id"))
            .query(io.sqm.dsl.Dsl.select(lit(1)).build())
            .onConflictDoNothing(Identifier.of("id"))
            .returning(col("id").toSelectItem())
            .build();

        assertEquals(InsertStatement.InsertMode.STANDARD, built.insertMode());
        assertEquals(1, built.columns().size());
        assertEquals(InsertStatement.OnConflictAction.DO_NOTHING, built.onConflictAction());
        assertEquals(1, built.returning().size());
        assertInstanceOf(io.sqm.core.SelectQuery.class, built.source());
    }

    @Test
    void builderSupportsListAndVarargConvenienceOverloads() {
        var statement = InsertStatement.builder(tbl("users"))
            .replace()
            .standard()
            .columns(java.util.List.of(Identifier.of("id"), Identifier.of("name")))
            .values(row(lit(1), lit("alice")))
            .onConflictDoUpdate(set("name", lit("alice2")))
            .onConflictDoNothing()
            .output(output(outputItem(inserted("id"))))
            .returning(java.util.List.of(col("id").toSelectItem()))
            .build();

        assertEquals(InsertStatement.InsertMode.STANDARD, statement.insertMode());
        assertEquals(2, statement.columns().size());
        assertEquals(InsertStatement.OnConflictAction.DO_NOTHING, statement.onConflictAction());
        assertTrue(statement.conflictTarget().isEmpty());
        assertTrue(statement.conflictUpdateAssignments().isEmpty());
        assertNull(statement.conflictUpdateWhere());
        assertEquals(OutputRowSource.INSERTED, statement.output().items().getFirst().expression().matchExpression().outputColumn(OutputColumnExpr::source).orElse(null));
        assertEquals(1, statement.returning().size());
    }

    @Test
    void canonicalFactorySupportsStandardModeWithoutColumns() {
        var statementWithColumns = InsertStatement.of(
            InsertStatement.InsertMode.STANDARD,
            tbl("users"),
            java.util.List.of(Identifier.of("id")),
            row(lit(1)),
            java.util.List.of(),
            InsertStatement.OnConflictAction.NONE,
            java.util.List.of(),
            null,
            null,
            java.util.List.of(col("id").toSelectItem()));
        var statementWithoutColumns = InsertStatement.of(
            InsertStatement.InsertMode.STANDARD,
            tbl("users"),
            java.util.List.of(),
            row(lit(1)),
            java.util.List.of(),
            InsertStatement.OnConflictAction.NONE,
            java.util.List.of(),
            null,
            null,
            java.util.List.of(col("id").toSelectItem()));

        assertEquals(InsertStatement.InsertMode.STANDARD, statementWithColumns.insertMode());
        assertEquals(1, statementWithColumns.columns().size());
        assertEquals(1, statementWithColumns.returning().size());
        assertEquals(InsertStatement.InsertMode.STANDARD, statementWithoutColumns.insertMode());
        assertTrue(statementWithoutColumns.columns().isEmpty());
        assertEquals(1, statementWithoutColumns.returning().size());
    }

    @Test
    void normalizesNullCollectionsInFactories() {
        var statement = InsertStatement.of(InsertStatement.InsertMode.IGNORE, tbl("users"), null, row(lit(1)), null, null, null, null, null, null);
        var statementWithReturningOverload = InsertStatement.of(
            InsertStatement.InsertMode.REPLACE,
            tbl("users"),
            java.util.List.of(),
            row(lit(1)),
            java.util.List.of(),
            InsertStatement.OnConflictAction.NONE,
            java.util.List.of(),
            null,
            null,
            null);

        assertEquals(InsertStatement.InsertMode.IGNORE, statement.insertMode());
        assertTrue(statement.columns().isEmpty());
        assertTrue(statement.conflictTarget().isEmpty());
        assertEquals(InsertStatement.OnConflictAction.NONE, statement.onConflictAction());
        assertTrue(statement.conflictUpdateAssignments().isEmpty());
        assertTrue(statement.returning().isEmpty());
        assertEquals(InsertStatement.InsertMode.REPLACE, statementWithReturningOverload.insertMode());
        assertTrue(statementWithReturningOverload.columns().isEmpty());
        assertTrue(statementWithReturningOverload.returning().isEmpty());
    }

    @Test
    void equalityAndHashDependOnShape() {
        var first = insert(tbl("users"))
            .ignore()
            .columns(Identifier.of("id"))
            .values(row(lit(1)))
            .onConflictDoNothing(Identifier.of("id"))
            .returning(col("id").toSelectItem())
            .build();
        var second = insert(tbl("users"))
            .ignore()
            .columns(Identifier.of("id"))
            .values(row(lit(1)))
            .onConflictDoNothing(Identifier.of("id"))
            .returning(col("id").toSelectItem())
            .build();
        var third = insert(tbl("users"))
            .columns(Identifier.of("id"))
            .values(row(lit(1)))
            .onConflictDoNothing(Identifier.of("id"))
            .returning(col("id").toSelectItem())
            .build();

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, third);
    }

    @Test
    void validatesRequiredMembers() {
        var defaultModeStatement = InsertStatement.of(null, tbl("users"), java.util.List.of(), row(lit(1)), java.util.List.of(), null, java.util.List.of(), null, null, java.util.List.of());
        assertEquals(InsertStatement.InsertMode.STANDARD, defaultModeStatement.insertMode());
        assertThrows(NullPointerException.class, () -> InsertStatement.of(InsertStatement.InsertMode.STANDARD, null, java.util.List.of(), row(lit(1)), java.util.List.of(), InsertStatement.OnConflictAction.NONE, java.util.List.of(), null, null, java.util.List.of()));
        assertThrows(NullPointerException.class, () -> InsertStatement.of(InsertStatement.InsertMode.STANDARD, tbl("users"), java.util.List.of(), null, java.util.List.of(), InsertStatement.OnConflictAction.NONE, java.util.List.of(), null, null, java.util.List.of()));
        assertThrows(IllegalStateException.class, () -> insert(tbl("users")).build());
        assertThrows(NullPointerException.class, () -> InsertStatement.builder(tbl("users")).table(null));
        assertThrows(NullPointerException.class, () -> InsertStatement.builder(tbl("users")).insertMode(null));
        assertThrows(NullPointerException.class, () -> InsertStatement.builder(tbl("users")).columns((Identifier[]) null));
        assertThrows(NullPointerException.class, () -> InsertStatement.builder(tbl("users")).source(null));
        assertThrows(NullPointerException.class, () -> InsertStatement.builder(tbl("users")).returning((SelectItem[]) null));
        assertThrows(IllegalArgumentException.class, () -> InsertStatement.of(InsertStatement.InsertMode.STANDARD, tbl("users"), java.util.List.of(), row(lit(1)), java.util.List.of(), InsertStatement.OnConflictAction.DO_UPDATE, java.util.List.of(), null, null, java.util.List.of()));
        assertThrows(IllegalArgumentException.class, () -> InsertStatement.of(InsertStatement.InsertMode.STANDARD, tbl("users"), java.util.List.of(), row(lit(1)), java.util.List.of(Identifier.of("id")), InsertStatement.OnConflictAction.NONE, java.util.List.of(), null, null, java.util.List.of()));
        assertThrows(IllegalArgumentException.class, () -> InsertStatement.of(InsertStatement.InsertMode.STANDARD, tbl("users"), java.util.List.of(), row(lit(1)), java.util.List.of(), InsertStatement.OnConflictAction.DO_NOTHING, java.util.List.of(set("id", lit(2))), null, null, java.util.List.of()));
    }
}
