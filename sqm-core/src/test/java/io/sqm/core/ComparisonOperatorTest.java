package io.sqm.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ComparisonOperator enum")
class ComparisonOperatorTest {

    @Test
    @DisplayName("EQ (equal) operator exists")
    void eq() {
        assertEquals(ComparisonOperator.EQ, ComparisonOperator.EQ);
        assertNotNull(ComparisonOperator.EQ);
    }

    @Test
    @DisplayName("NE (not equal) operator exists")
    void ne() {
        assertEquals(ComparisonOperator.NE, ComparisonOperator.NE);
        assertNotNull(ComparisonOperator.NE);
    }

    @Test
    @DisplayName("LT (less than) operator exists")
    void lt() {
        assertEquals(ComparisonOperator.LT, ComparisonOperator.LT);
        assertNotNull(ComparisonOperator.LT);
    }

    @Test
    @DisplayName("LTE (less than or equal) operator exists")
    void lte() {
        assertEquals(ComparisonOperator.LTE, ComparisonOperator.LTE);
        assertNotNull(ComparisonOperator.LTE);
    }

    @Test
    @DisplayName("GT (greater than) operator exists")
    void gt() {
        assertEquals(ComparisonOperator.GT, ComparisonOperator.GT);
        assertNotNull(ComparisonOperator.GT);
    }

    @Test
    @DisplayName("GTE (greater than or equal) operator exists")
    void gte() {
        assertEquals(ComparisonOperator.GTE, ComparisonOperator.GTE);
        assertNotNull(ComparisonOperator.GTE);
    }

    @Test
    @DisplayName("All operators are distinct")
    void allOperatorsDistinct() {
        ComparisonOperator[] operators = ComparisonOperator.values();
        assertEquals(6, operators.length);
        assertNotEquals(ComparisonOperator.EQ, ComparisonOperator.NE);
        assertNotEquals(ComparisonOperator.LT, ComparisonOperator.GT);
        assertNotEquals(ComparisonOperator.LTE, ComparisonOperator.GTE);
    }

    @Test
    @DisplayName("valueOf() works for valid operator strings")
    void valueOfValid() {
        assertEquals(ComparisonOperator.EQ, ComparisonOperator.valueOf("EQ"));
        assertEquals(ComparisonOperator.NE, ComparisonOperator.valueOf("NE"));
        assertEquals(ComparisonOperator.LT, ComparisonOperator.valueOf("LT"));
        assertEquals(ComparisonOperator.LTE, ComparisonOperator.valueOf("LTE"));
        assertEquals(ComparisonOperator.GT, ComparisonOperator.valueOf("GT"));
        assertEquals(ComparisonOperator.GTE, ComparisonOperator.valueOf("GTE"));
    }

    @Test
    @DisplayName("valueOf() throws for invalid operator string")
    void valueOfInvalid() {
        assertThrows(IllegalArgumentException.class, () -> ComparisonOperator.valueOf("INVALID"));
        assertThrows(IllegalArgumentException.class, () -> ComparisonOperator.valueOf("eq")); // lowercase
        assertThrows(IllegalArgumentException.class, () -> ComparisonOperator.valueOf("GREATER"));
    }

    @Test
    @DisplayName("Operator name() returns correct string")
    void operatorName() {
        assertEquals("EQ", ComparisonOperator.EQ.name());
        assertEquals("NE", ComparisonOperator.NE.name());
        assertEquals("LT", ComparisonOperator.LT.name());
        assertEquals("LTE", ComparisonOperator.LTE.name());
        assertEquals("GT", ComparisonOperator.GT.name());
        assertEquals("GTE", ComparisonOperator.GTE.name());
    }

    @Test
    @DisplayName("Operator ordinal() returns correct position")
    void operatorOrdinal() {
        assertEquals(0, ComparisonOperator.EQ.ordinal());
        assertEquals(1, ComparisonOperator.NE.ordinal());
        assertEquals(2, ComparisonOperator.LT.ordinal());
        assertEquals(3, ComparisonOperator.LTE.ordinal());
        assertEquals(4, ComparisonOperator.GT.ordinal());
        assertEquals(5, ComparisonOperator.GTE.ordinal());
    }

    @Test
    @DisplayName("Comparison operators can be used in switch statements")
    void switchUsage() {
        ComparisonOperator op = ComparisonOperator.EQ;
        String description = switch (op) {
            case EQ -> "equal";
            case NE -> "not equal";
            case LT -> "less than";
            case LTE -> "less than or equal";
            case GT -> "greater than";
            case GTE -> "greater than or equal";
        };
        assertEquals("equal", description);

        op = ComparisonOperator.GT;
        description = switch (op) {
            case EQ -> "equal";
            case NE -> "not equal";
            case LT -> "less than";
            case LTE -> "less than or equal";
            case GT -> "greater than";
            case GTE -> "greater than or equal";
        };
        assertEquals("greater than", description);
    }

    @Test
    @DisplayName("values() returns all operators")
    void valuesMethod() {
        ComparisonOperator[] values = ComparisonOperator.values();
        assertEquals(6, values.length);
        assertTrue(contains(values, ComparisonOperator.EQ));
        assertTrue(contains(values, ComparisonOperator.NE));
        assertTrue(contains(values, ComparisonOperator.LT));
        assertTrue(contains(values, ComparisonOperator.LTE));
        assertTrue(contains(values, ComparisonOperator.GT));
        assertTrue(contains(values, ComparisonOperator.GTE));
    }

    @Test
    @DisplayName("Comparison operators represent typical SQL operators")
    void representsSqlOperators() {
        // EQ represents = operator
        // NE represents <> or != operator
        // LT represents < operator
        // LTE represents <= operator
        // GT represents > operator
        // GTE represents >= operator
        assertEquals(6, ComparisonOperator.values().length);
    }

    @Test
    @DisplayName("Each operator is unique by ordinal")
    void uniqueByOrdinal() {
        ComparisonOperator[] values = ComparisonOperator.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertNotEquals(values[i].ordinal(), values[j].ordinal());
            }
        }
    }

    private boolean contains(ComparisonOperator[] array, ComparisonOperator operator) {
        for (ComparisonOperator op : array) {
            if (op == operator) {
                return true;
            }
        }
        return false;
    }
}
