package io.sqm.core;

import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.insert;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.rows;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InsertStatementTest {

    @Test
    void builderCreatesImmutableInsertStatement() {
        var statement = insert(tbl("users"))
            .columns(Identifier.of("id"), Identifier.of("name"))
            .values(rows(
                row(lit(1), lit("alice")),
                row(lit(2), lit("bob"))))
            .returning(col("id").toSelectItem())
            .build();

        assertEquals("users", statement.table().name().value());
        assertEquals(2, statement.columns().size());
        assertInstanceOf(RowListExpr.class, statement.source());
        assertEquals(1, statement.returning().size());
        assertThrows(UnsupportedOperationException.class, () -> statement.columns().add(Identifier.of("x")));
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
            .returning(col("id").toSelectItem())
            .build();

        assertEquals(1, built.columns().size());
        assertEquals(1, built.returning().size());
        assertInstanceOf(io.sqm.core.SelectQuery.class, built.source());
    }

    @Test
    void equalityAndHashDependOnShape() {
        var first = insert(tbl("users"))
            .columns(Identifier.of("id"))
            .values(row(lit(1)))
            .returning(col("id").toSelectItem())
            .build();
        var second = insert(tbl("users"))
            .columns(Identifier.of("id"))
            .values(row(lit(1)))
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
    }
}
