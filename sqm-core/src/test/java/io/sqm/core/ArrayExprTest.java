package io.sqm.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static io.sqm.dsl.Dsl.type;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ArrayExpr")
class ArrayExprTest {

    @Test
    @DisplayName("Create array with variable arguments")
    void createArrayWithVarargs() {
        Expression lit1 = Expression.literal("a");
        Expression lit2 = Expression.literal("b");
        ArrayExpr arr = ArrayExpr.of(lit1, lit2);
        assertEquals(2, arr.elements().size());
        assertEquals(lit1, arr.elements().get(0));
        assertEquals(lit2, arr.elements().get(1));
    }

    @Test
    @DisplayName("Create array with list argument")
    void createArrayWithList() {
        List<Expression> elements = List.of(
            Expression.literal(1),
            Expression.literal(2),
            Expression.literal(3)
        );
        ArrayExpr arr = ArrayExpr.of(elements);
        assertEquals(3, arr.elements().size());
        assertEquals(elements, arr.elements());
    }

    @Test
    @DisplayName("Create empty array")
    void createEmptyArray() {
        ArrayExpr arr = ArrayExpr.of();
        assertEquals(0, arr.elements().size());
    }

    @Test
    @DisplayName("Array is instance of Expression")
    void isExpression() {
        ArrayExpr arr = ArrayExpr.of(Expression.literal(1));
        assertInstanceOf(Expression.class, arr);
    }

    @Test
    @DisplayName("Array is instance of Node")
    void isNode() {
        ArrayExpr arr = ArrayExpr.of(Expression.literal(1));
        assertInstanceOf(Node.class, arr);
    }

    @Test
    @DisplayName("Array with mixed element types")
    void mixedElementTypes() {
        ArrayExpr arr = ArrayExpr.of(
            Expression.literal(1),
            ColumnExpr.of(null, Identifier.of("col")),
            Expression.literal("text")
        );
        assertEquals(3, arr.elements().size());
    }

    @Test
    @DisplayName("Array with nested array elements")
    void nestedArrayElements() {
        ArrayExpr inner = ArrayExpr.of(Expression.literal(1), Expression.literal(2));
        ArrayExpr outer = ArrayExpr.of(inner, Expression.literal(3));
        assertEquals(2, outer.elements().size());
        assertInstanceOf(ArrayExpr.class, outer.elements().getFirst());
    }

    @Test
    @DisplayName("Array with cast expressions")
    void arrayWithCastExpressions() {
        CastExpr cast1 = CastExpr.of(Expression.literal("123"), type("integer"));
        CastExpr cast2 = CastExpr.of(Expression.literal("456"), type("integer"));
        ArrayExpr arr = ArrayExpr.of(cast1, cast2);
        assertEquals(2, arr.elements().size());
    }

    @Test
    @DisplayName("Array with column expressions")
    void arrayWithColumnExpressions() {
        ColumnExpr col1 = ColumnExpr.of(Identifier.of("users"), Identifier.of("id"));
        ColumnExpr col2 = ColumnExpr.of(Identifier.of("users"), Identifier.of("name"));
        ArrayExpr arr = ArrayExpr.of(col1, col2);
        assertEquals(2, arr.elements().size());
        assertEquals(col1, arr.elements().get(0));
        assertEquals(col2, arr.elements().get(1));
    }

    @Test
    @DisplayName("Array elements are immutable")
    void elementsImmutable() {
        ArrayExpr arr = ArrayExpr.of(Expression.literal(1), Expression.literal(2));
        List<Expression> elements = arr.elements();
        assertThrows(UnsupportedOperationException.class, 
            () -> elements.add(Expression.literal(3)));
    }

    @Test
    @DisplayName("Arrays with same elements are equal")
    void equalityWithSameElements() {
        Expression lit1 = Expression.literal(1);
        Expression lit2 = Expression.literal(2);
        ArrayExpr arr1 = ArrayExpr.of(lit1, lit2);
        ArrayExpr arr2 = ArrayExpr.of(Expression.literal(1), Expression.literal(2));
        assertEquals(arr1, arr2);
    }

    @Test
    @DisplayName("Arrays with different elements are not equal")
    void inequalityWithDifferentElements() {
        ArrayExpr arr1 = ArrayExpr.of(Expression.literal(1), Expression.literal(2));
        ArrayExpr arr2 = ArrayExpr.of(Expression.literal(1), Expression.literal(3));
        assertNotEquals(arr1, arr2);
    }

    @Test
    @DisplayName("Arrays with different element count are not equal")
    void inequalityWithDifferentCount() {
        ArrayExpr arr1 = ArrayExpr.of(Expression.literal(1), Expression.literal(2));
        ArrayExpr arr2 = ArrayExpr.of(Expression.literal(1));
        assertNotEquals(arr1, arr2);
    }

    @Test
    @DisplayName("Array with single element")
    void singleElementArray() {
        Expression lit = Expression.literal("single");
        ArrayExpr arr = ArrayExpr.of(lit);
        assertEquals(1, arr.elements().size());
        assertEquals(lit, arr.elements().getFirst());
    }

    @Test
    @DisplayName("Array with many elements")
    void manyElementsArray() {
        Expression[] literals = new Expression[100];
        for (int i = 0; i < 100; i++) {
            literals[i] = Expression.literal(i);
        }
        ArrayExpr arr = ArrayExpr.of(literals);
        assertEquals(100, arr.elements().size());
    }

    @Test
    @DisplayName("Array can be used in comparison")
    void arrayInComparison() {
        ArrayExpr arr = ArrayExpr.of(Expression.literal(1), Expression.literal(2));
        Predicate pred = arr.eq(ArrayExpr.of(Expression.literal(1), Expression.literal(2)));
        assertNotNull(pred);
        assertInstanceOf(Predicate.class, pred);
    }
}
