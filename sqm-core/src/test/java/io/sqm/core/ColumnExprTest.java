package io.sqm.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ColumnExpr")
class ColumnExprTest {

    @Test
    @DisplayName("Create column with name only (no table alias)")
    void ofNameOnly() {
        ColumnExpr col = ColumnExpr.of(null, Identifier.of("id"));
        assertEquals("id", col.name().value());
        assertNull(col.tableAlias());
    }

    @Test
    @DisplayName("Create column with table alias and name")
    void ofWithTableAlias() {
        ColumnExpr col = ColumnExpr.of(Identifier.of("users"), Identifier.of("id"));
        assertEquals("id", col.name().value());
        assertEquals("users", col.tableAlias().value());
    }

    @Test
    @DisplayName("Create column with null table alias")
    void ofWithNullTableAlias() {
        ColumnExpr col = ColumnExpr.of(null, Identifier.of("id"));
        assertEquals("id", col.name().value());
        assertNull(col.tableAlias());
    }

    @Test
    @DisplayName("Column is instance of Expression")
    void isExpression() {
        ColumnExpr col = ColumnExpr.of(null, Identifier.of("id"));
        assertInstanceOf(Expression.class, col);
    }

    @Test
    @DisplayName("Column is instance of Node")
    void isNode() {
        ColumnExpr col = ColumnExpr.of(null, Identifier.of("id"));
        assertInstanceOf(Node.class, col);
    }

    @Test
    @DisplayName("inTable() adds table alias to column")
    void inTable() {
        ColumnExpr col = ColumnExpr.of(null, Identifier.of("id"));
        ColumnExpr withTable = col.inTable("users");
        assertEquals("id", withTable.name().value());
        assertEquals("users", withTable.tableAlias().value());
    }

    @Test
    @DisplayName("inTable() on column with existing alias replaces it")
    void inTableReplaceExisting() {
        ColumnExpr col = ColumnExpr.of(Identifier.of("old_alias"), Identifier.of("id"));
        ColumnExpr updated = col.inTable("new_alias");
        assertEquals("id", updated.name().value());
        assertEquals("new_alias", updated.tableAlias().value());
        assertEquals("old_alias", col.tableAlias().value()); // Original unchanged
    }

    @Test
    @DisplayName("Different columns with same name are equal")
    void equalityByName() {
        ColumnExpr col1 = ColumnExpr.of(null, Identifier.of("id"));
        ColumnExpr col2 = ColumnExpr.of(null, Identifier.of("id"));
        assertEquals(col1, col2);
    }

    @Test
    @DisplayName("Columns with different names are not equal")
    void inequalityDifferentName() {
        ColumnExpr col1 = ColumnExpr.of(null, Identifier.of("id"));
        ColumnExpr col2 = ColumnExpr.of(null, Identifier.of("name"));
        assertNotEquals(col1, col2);
    }

    @Test
    @DisplayName("Columns with different table aliases are not equal")
    void inequalityDifferentTable() {
        ColumnExpr col1 = ColumnExpr.of(Identifier.of("users"), Identifier.of("id"));
        ColumnExpr col2 = ColumnExpr.of(Identifier.of("orders"), Identifier.of("id"));
        assertNotEquals(col1, col2);
    }

    @Test
    @DisplayName("Column with table alias and without are not equal")
    void inequalityOneWithTableAlias() {
        ColumnExpr col1 = ColumnExpr.of(Identifier.of("users"), Identifier.of("id"));
        ColumnExpr col2 = ColumnExpr.of(null, Identifier.of("id"));
        assertNotEquals(col1, col2);
    }

    @Test
    @DisplayName("Column names with special characters")
    void specialCharacterNames() {
        ColumnExpr col = ColumnExpr.of(null, Identifier.of("user_id"));
        assertEquals("user_id", col.name().value());

        col = ColumnExpr.of(null, Identifier.of("user-id"));
        assertEquals("user-id", col.name().value());

        col = ColumnExpr.of(null, Identifier.of("`user-id`"));
        assertEquals("`user-id`", col.name().value());
    }

    @Test
    @DisplayName("Multiple columns can reference same table")
    void multipleColumnsFromSameTable() {
        ColumnExpr id = ColumnExpr.of(Identifier.of("users"), Identifier.of("id"));
        ColumnExpr name = ColumnExpr.of(Identifier.of("users"), Identifier.of("name"));
        assertEquals("users", id.tableAlias().value());
        assertEquals("users", name.tableAlias().value());
        assertNotEquals(id.name().value(), name.name().value());
    }

    @Test
    @DisplayName("Column expression can be used in comparison")
    void usageInComparison() {
        ColumnExpr col = ColumnExpr.of(Identifier.of("users"), Identifier.of("age"));
        ComparisonPredicate pred = col.gt(Expression.literal(18));
        assertNotNull(pred);
        assertInstanceOf(Predicate.class, pred);
    }

    @Test
    @DisplayName("Column expression can be used in arithmetic")
    void usageInArithmetic() {
        ColumnExpr col = ColumnExpr.of(null, Identifier.of("salary"));
        BinaryArithmeticExpr expr = col.add(Expression.literal(1000));
        assertNotNull(expr);
        assertInstanceOf(Expression.class, expr);
    }

    @Test
    @DisplayName("Preserves identifier quote metadata")
    void quoteMetadata() {
        ColumnExpr col = ColumnExpr.of(
            Identifier.of("U", QuoteStyle.DOUBLE_QUOTE),
            Identifier.of("Name", QuoteStyle.BACKTICK)
        );
        assertEquals("U", col.tableAlias().value());
        assertEquals("Name", col.name().value());
        assertEquals(QuoteStyle.DOUBLE_QUOTE, col.tableAlias().quoteStyle());
        assertEquals(QuoteStyle.BACKTICK, col.name().quoteStyle());
    }
}
