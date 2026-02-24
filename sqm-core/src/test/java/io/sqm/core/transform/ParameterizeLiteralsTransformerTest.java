package io.sqm.core.transform;

import io.sqm.core.*;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link ParameterizeLiteralsTransformer}.
 * Tests the transformation of literal expressions into parameterized forms.
 */
class ParameterizeLiteralsTransformerTest {

    @Test
    void parameterizeSingleLiteral() {
        LiteralExpr literal = lit(42);
        ParameterizeLiteralsTransformer transformer = new ParameterizeLiteralsTransformer(i -> param(i));

        Node result = literal.accept(transformer);
        assertNotNull(result);
        assertInstanceOf(ParamExpr.class, result);
        assertEquals(1, transformer.values().size());
        assertEquals(42, transformer.values().getFirst());
    }

    @Test
    void parameterizeSingleStringLiteral() {
        LiteralExpr literal = lit("hello");
        ParameterizeLiteralsTransformer transformer = new ParameterizeLiteralsTransformer(i -> param(i));

        Node result = literal.accept(transformer);
        assertNotNull(result);
        assertInstanceOf(ParamExpr.class, result);
        assertEquals(1, transformer.values().size());
        assertEquals("hello", transformer.values().getFirst());
    }

    @Test
    void parameterizeSingleNullLiteral() {
        LiteralExpr literal = lit(null);
        ParameterizeLiteralsTransformer transformer = new ParameterizeLiteralsTransformer(i -> param(i));

        Node result = literal.accept(transformer);
        assertNotNull(result);
        assertInstanceOf(ParamExpr.class, result);
        assertEquals(1, transformer.values().size());
        assertNull(transformer.values().getFirst());
    }

    @Test
    void parameterizeDoubleValue() {
        LiteralExpr literal = lit(3.14);
        ParameterizeLiteralsTransformer transformer = new ParameterizeLiteralsTransformer(i -> param(i));

        Node result = literal.accept(transformer);
        assertNotNull(result);
        assertInstanceOf(ParamExpr.class, result);
        assertEquals(1, transformer.values().size());
        assertEquals(3.14, (Double) transformer.values().getFirst(), 0.001);
    }

    @Test
    void parameterizeBooleanValue() {
        LiteralExpr literal = lit(true);
        ParameterizeLiteralsTransformer transformer = new ParameterizeLiteralsTransformer(i -> param(i));

        Node result = literal.accept(transformer);
        assertNotNull(result);
        assertInstanceOf(ParamExpr.class, result);
        assertEquals(1, transformer.values().size());
        assertEquals(true, transformer.values().getFirst());
    }

    @Test
    void parameterizeMultipleLiteralsInAddition() {
        AddArithmeticExpr expr = lit(10).add(lit(20));
        ParameterizeLiteralsTransformer transformer = new ParameterizeLiteralsTransformer(i -> param(i));

        Node result = expr.accept(transformer);
        assertNotNull(result);
        assertInstanceOf(AddArithmeticExpr.class, result);

        assertEquals(2, transformer.values().size());
        assertEquals(10, transformer.values().get(0));
        assertEquals(20, transformer.values().get(1));
    }

    @Test
    void parameterizeMultipleLiteralsInSubtraction() {
        SubArithmeticExpr expr = lit(100).sub(lit(50));
        ParameterizeLiteralsTransformer transformer = new ParameterizeLiteralsTransformer(i -> param(i));

        Node result = expr.accept(transformer);
        assertNotNull(result);
        assertInstanceOf(SubArithmeticExpr.class, result);

        assertEquals(2, transformer.values().size());
        assertEquals(100, transformer.values().get(0));
        assertEquals(50, transformer.values().get(1));
    }

    @Test
    void parameterizeComplexArithmeticExpression() {
        // Create: (10 + 20) * (30 - 5)
        Expression left = lit(10).add(lit(20));
        Expression right = lit(30).sub(lit(5));
        Expression expr = left.mul(right);

        ParameterizeLiteralsTransformer transformer = new ParameterizeLiteralsTransformer(i -> param(i));
        Node result = expr.accept(transformer);
        assertNotNull(result);

        // Should have 4 parameters
        assertEquals(4, transformer.values().size());
        assertEquals(10, transformer.values().get(0));
        assertEquals(20, transformer.values().get(1));
        assertEquals(30, transformer.values().get(2));
        assertEquals(5, transformer.values().get(3));
    }

    @Test
    void parameterizeLiteralInComparisonPredicate() {
        ComparisonPredicate pred = col("id").eq(lit(42));
        ParameterizeLiteralsTransformer transformer = new ParameterizeLiteralsTransformer(i -> param(i));

        Node result = pred.accept(transformer);
        assertNotNull(result);
        assertInstanceOf(ComparisonPredicate.class, result);

        assertEquals(1, transformer.values().size());
        assertEquals(42, transformer.values().getFirst());
    }

    @Test
    void parameterizeLiteralsInMultipleComparison() {
        // Create: col = 10 AND col2 = 'active'
        Predicate pred1 = col("id").eq(lit(10));
        Predicate pred2 = col("status").eq(lit("active"));
        Predicate combined = pred1.and(pred2);

        ParameterizeLiteralsTransformer transformer = new ParameterizeLiteralsTransformer(i -> param(i));
        Node result = combined.accept(transformer);
        assertNotNull(result);

        assertEquals(2, transformer.values().size());
        assertEquals(10, transformer.values().get(0));
        assertEquals("active", transformer.values().get(1));
    }

    @Test
    void parameterizeLiteralInBetweenPredicate() {
        BetweenPredicate pred = col("age").between(lit(18), lit(65));
        ParameterizeLiteralsTransformer transformer = new ParameterizeLiteralsTransformer(i -> param(i));

        Node result = pred.accept(transformer);
        assertNotNull(result);
        assertInstanceOf(BetweenPredicate.class, result);

        assertEquals(2, transformer.values().size());
        assertEquals(18, transformer.values().get(0));
        assertEquals(65, transformer.values().get(1));
    }

    @Test
    void parameterizeLiteralInInPredicate() {
        InPredicate pred = col("status").in(row(lit("active"), lit("pending")));
        ParameterizeLiteralsTransformer transformer = new ParameterizeLiteralsTransformer(i -> param(i));

        Node result = pred.accept(transformer);
        assertNotNull(result);
        assertInstanceOf(InPredicate.class, result);

        assertEquals(2, transformer.values().size());
        assertEquals("active", transformer.values().get(0));
        assertEquals("pending", transformer.values().get(1));
    }

    @Test
    void parameterizeLiteralInFunctionCall() {
        FunctionExpr func = func("CONCAT", arg(col("first_name")), arg(lit(" ")), arg(col("last_name")));
        ParameterizeLiteralsTransformer transformer = new ParameterizeLiteralsTransformer(i -> param(i));

        Node result = func.accept(transformer);
        assertNotNull(result);
        assertInstanceOf(FunctionExpr.class, result);

        assertEquals(1, transformer.values().size());
        assertEquals(" ", transformer.values().getFirst());
    }

    @Test
    void parameterizeLiteralsInCaseWhen() {
        CaseExpr caseExpr = kase(
            when(col("status").eq(lit("active"))).then(lit(1))
        );

        ParameterizeLiteralsTransformer transformer = new ParameterizeLiteralsTransformer(i -> param(i));
        Node result = caseExpr.accept(transformer);
        assertNotNull(result);
        assertInstanceOf(CaseExpr.class, result);

        assertEquals(2, transformer.values().size());
        assertEquals("active", transformer.values().get(0));
        assertEquals(1, transformer.values().get(1));
    }

    @Test
    void parameterizeLiteralsInSelectQuery() {
        SelectQuery query = select(col("id"), lit(42).as("constant"))
            .from(tbl("users"))
            .where(col("age").gte(lit(18)))
            .build();

        ParameterizeLiteralsTransformer transformer = new ParameterizeLiteralsTransformer(i -> param(i));
        Node result = query.accept(transformer);
        assertNotNull(result);

        assertEquals(2, transformer.values().size());
        assertEquals(42, transformer.values().get(0));
        assertEquals(18, transformer.values().get(1));
    }

    @Test
    void preserveColumnExpressionStructure() {
        ColumnExpr col = col("user_id");
        ParameterizeLiteralsTransformer transformer = new ParameterizeLiteralsTransformer(i -> param(i));

        Node result = col.accept(transformer);
        assertNotNull(result);
        assertInstanceOf(ColumnExpr.class, result);
        assertEquals("user_id", ((ColumnExpr) result).name().value());
    }

    @Test
    void parameterCreatorReceivesIncrementingIndex() {
        java.util.List<Integer> receivedIndices = new java.util.ArrayList<>();
        ParameterizeLiteralsTransformer transformer = new ParameterizeLiteralsTransformer(i -> {
            receivedIndices.add(i);
            return param(i);
        });

        lit(1).add(lit(2)).mul(lit(3)).accept(transformer);

        assertEquals(3, receivedIndices.size());
        assertEquals(1, (int) receivedIndices.get(0));
        assertEquals(2, (int) receivedIndices.get(1));
        assertEquals(3, (int) receivedIndices.get(2));
    }

    @Test
    void orderingOfParametersMatchesEncounterOrder() {
        SelectQuery query = select(lit("a"), lit("b"), lit("c")).from(tbl("t")).build();
        ParameterizeLiteralsTransformer transformer = new ParameterizeLiteralsTransformer(i -> param(i));

        query.accept(transformer);

        assertEquals(3, transformer.values().size());
        assertEquals("a", transformer.values().get(0));
        assertEquals("b", transformer.values().get(1));
        assertEquals("c", transformer.values().get(2));
    }

    @Test
    void valuesListIsUnmodifiable() {
        LiteralExpr literal = lit(42);
        ParameterizeLiteralsTransformer transformer = new ParameterizeLiteralsTransformer(i -> param(i));
        literal.accept(transformer);

        var list = transformer.values();
        //noinspection DataFlowIssue
        assertThrows(UnsupportedOperationException.class, () -> list.add(100));
    }
}
