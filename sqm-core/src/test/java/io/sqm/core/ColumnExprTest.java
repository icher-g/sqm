package io.sqm.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ColumnExpr")
class ColumnExprTest {

    @Test
    @DisplayName("Create column with name only (no table alias)")
    void ofNameOnly() {
        ColumnExpr col = ColumnExpr.of("id");
        assertEquals("id", col.name());
        assertNull(col.tableAlias());
    }

    @Test
    @DisplayName("Create column with table alias and name")
    void ofWithTableAlias() {
        ColumnExpr col = ColumnExpr.of("users", "id");
        assertEquals("id", col.name());
        assertEquals("users", col.tableAlias());
    }

    @Test
    @DisplayName("Create column with null table alias")
    void ofWithNullTableAlias() {
        ColumnExpr col = ColumnExpr.of(null, "id");
        assertEquals("id", col.name());
        assertNull(col.tableAlias());
    }

    @Test
    @DisplayName("Column is instance of Expression")
    void isExpression() {
        ColumnExpr col = ColumnExpr.of("id");
        assertInstanceOf(Expression.class, col);
    }

    @Test
    @DisplayName("Column is instance of Node")
    void isNode() {
        ColumnExpr col = ColumnExpr.of("id");
        assertInstanceOf(Node.class, col);
    }

    @Test
    @DisplayName("inTable() adds table alias to column")
    void inTable() {
        ColumnExpr col = ColumnExpr.of("id");
        ColumnExpr withTable = col.inTable("users");
        assertEquals("id", withTable.name());
        assertEquals("users", withTable.tableAlias());
    }

    @Test
    @DisplayName("inTable() on column with existing alias replaces it")
    void inTableReplaceExisting() {
        ColumnExpr col = ColumnExpr.of("old_alias", "id");
        ColumnExpr updated = col.inTable("new_alias");
        assertEquals("id", updated.name());
        assertEquals("new_alias", updated.tableAlias());
        assertEquals("old_alias", col.tableAlias()); // Original unchanged
    }

    @Test
    @DisplayName("Different columns with same name are equal")
    void equalityByName() {
        ColumnExpr col1 = ColumnExpr.of("id");
        ColumnExpr col2 = ColumnExpr.of("id");
        assertEquals(col1, col2);
    }

    @Test
    @DisplayName("Columns with different names are not equal")
    void inequalityDifferentName() {
        ColumnExpr col1 = ColumnExpr.of("id");
        ColumnExpr col2 = ColumnExpr.of("name");
        assertNotEquals(col1, col2);
    }

    @Test
    @DisplayName("Columns with different table aliases are not equal")
    void inequalityDifferentTable() {
        ColumnExpr col1 = ColumnExpr.of("users", "id");
        ColumnExpr col2 = ColumnExpr.of("orders", "id");
        assertNotEquals(col1, col2);
    }

    @Test
    @DisplayName("Column with table alias and without are not equal")
    void inequalityOneWithTableAlias() {
        ColumnExpr col1 = ColumnExpr.of("users", "id");
        ColumnExpr col2 = ColumnExpr.of("id");
        assertNotEquals(col1, col2);
    }

    @Test
    @DisplayName("Column names with special characters")
    void specialCharacterNames() {
        ColumnExpr col = ColumnExpr.of("user_id");
        assertEquals("user_id", col.name());

        col = ColumnExpr.of("user-id");
        assertEquals("user-id", col.name());

        col = ColumnExpr.of("`user-id`");
        assertEquals("`user-id`", col.name());
    }

    @Test
    @DisplayName("Multiple columns can reference same table")
    void multipleColumnsFromSameTable() {
        ColumnExpr id = ColumnExpr.of("users", "id");
        ColumnExpr name = ColumnExpr.of("users", "name");
        assertEquals("users", id.tableAlias());
        assertEquals("users", name.tableAlias());
        assertNotEquals(id.name(), name.name());
    }

    @Test
    @DisplayName("Column expression can be used in comparison")
    void usageInComparison() {
        ColumnExpr col = ColumnExpr.of("users", "age");
        ComparisonPredicate pred = col.gt(Expression.literal(18));
        assertNotNull(pred);
        assertInstanceOf(Predicate.class, pred);
    }

    @Test
    @DisplayName("Column expression can be used in arithmetic")
    void usageInArithmetic() {
        ColumnExpr col = ColumnExpr.of("salary");
        BinaryArithmeticExpr expr = col.add(Expression.literal(1000));
        assertNotNull(expr);
        assertInstanceOf(Expression.class, expr);
    }
}
