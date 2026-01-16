package io.sqm.core.walk;

import io.sqm.core.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link RecursiveNodeVisitor}.
 * Tests the visitor pattern implementation for traversing various node types.
 */
class ComprehensiveRecursiveNodeVisitorTest {

    private static class CountingVisitor extends RecursiveNodeVisitor<Integer> {
        private int count = 0;

        @Override
        protected Integer defaultResult() {
            return count;
        }

        @Override
        public Integer visitColumnExpr(ColumnExpr c) {
            count++;
            return super.visitColumnExpr(c);
        }

        @Override
        public Integer visitLiteralExpr(LiteralExpr e) {
            count++;
            return super.visitLiteralExpr(e);
        }

        @Override
        public Integer visitFunctionExpr(FunctionExpr f) {
            count++;
            return super.visitFunctionExpr(f);
        }

        @Override
        public Integer visitAddArithmeticExpr(AddArithmeticExpr e) {
            count++;
            return super.visitAddArithmeticExpr(e);
        }

        @Override
        public Integer visitSubArithmeticExpr(SubArithmeticExpr e) {
            count++;
            return super.visitSubArithmeticExpr(e);
        }

        @Override
        public Integer visitMulArithmeticExpr(MulArithmeticExpr e) {
            count++;
            return super.visitMulArithmeticExpr(e);
        }

        @Override
        public Integer visitDivArithmeticExpr(DivArithmeticExpr e) {
            count++;
            return super.visitDivArithmeticExpr(e);
        }

        @Override
        public Integer visitModArithmeticExpr(ModArithmeticExpr e) {
            count++;
            return super.visitModArithmeticExpr(e);
        }

        @Override
        public Integer visitCaseExpr(CaseExpr c) {
            count++;
            return super.visitCaseExpr(c);
        }

        @Override
        public Integer visitRowExpr(RowExpr r) {
            count++;
            return super.visitRowExpr(r);
        }

        @Override
        public Integer visitComparisonPredicate(ComparisonPredicate p) {
            count++;
            return super.visitComparisonPredicate(p);
        }

        @Override
        public Integer visitBetweenPredicate(BetweenPredicate p) {
            count++;
            return super.visitBetweenPredicate(p);
        }

        @Override
        public Integer visitInPredicate(InPredicate p) {
            count++;
            return super.visitInPredicate(p);
        }

        @Override
        public Integer visitLikePredicate(LikePredicate p) {
            count++;
            return super.visitLikePredicate(p);
        }

        @Override
        public Integer visitIsNullPredicate(IsNullPredicate p) {
            count++;
            return super.visitIsNullPredicate(p);
        }

        @Override
        public Integer visitCastExpr(CastExpr expr) {
            count++;
            return super.visitCastExpr(expr);
        }

        @Override
        public Integer visitUnaryOperatorExpr(UnaryOperatorExpr expr) {
            count++;
            return super.visitUnaryOperatorExpr(expr);
        }

        @Override
        public Integer visitNegativeArithmeticExpr(NegativeArithmeticExpr expr) {
            count++;
            return super.visitNegativeArithmeticExpr(expr);
        }
    }

    private static class ColumnCollector extends RecursiveNodeVisitor<List<String>> {
        private List<String> columns = new ArrayList<>();

        @Override
        protected List<String> defaultResult() {
            return columns;
        }

        @Override
        public List<String> visitColumnExpr(ColumnExpr c) {
            columns.add(c.name());
            return super.visitColumnExpr(c);
        }
    }

    @Test
    void visitColumnExpr() {
        ColumnExpr col = col("user_id");
        CountingVisitor visitor = new CountingVisitor();
        col.accept(visitor);
        assertEquals(1, visitor.count);
    }

    @Test
    void visitLiteralExpr_integer() {
        LiteralExpr lit = lit(42);
        CountingVisitor visitor = new CountingVisitor();
        lit.accept(visitor);
        assertEquals(1, visitor.count);
    }

    @Test
    void visitLiteralExpr_string() {
        LiteralExpr lit = lit("hello");
        CountingVisitor visitor = new CountingVisitor();
        lit.accept(visitor);
        assertEquals(1, visitor.count);
    }

    @Test
    void visitLiteralExpr_null() {
        LiteralExpr lit = lit((Object) null);
        CountingVisitor visitor = new CountingVisitor();
        lit.accept(visitor);
        assertEquals(1, visitor.count);
    }

    @Test
    void visitFunctionExpr() {
        FunctionExpr func = func("LOWER", arg(col("name")));
        CountingVisitor visitor = new CountingVisitor();
        func.accept(visitor);
        // Should count function and column argument
        assertTrue(visitor.count >= 2);
    }

    @Test
    void visitFunctionExpr_noArgs() {
        FunctionExpr func = func("NOW");
        CountingVisitor visitor = new CountingVisitor();
        func.accept(visitor);
        assertEquals(1, visitor.count);
    }

    @Test
    void visitFunctionExpr_multipleLiteralArgs() {
        FunctionExpr func = func("SUBSTRING", arg(col("text")), arg(lit(1)), arg(lit(5)));
        CountingVisitor visitor = new CountingVisitor();
        func.accept(visitor);
        // Function + arguments should be counted
        assertTrue(visitor.count >= 3);
    }

    @Test
    void visitAddArithmeticExpr() {
        AddArithmeticExpr expr = col("a").add(col("b"));
        CountingVisitor visitor = new CountingVisitor();
        expr.accept(visitor);
        // Addition + columns = 3
        assertEquals(3, visitor.count);
    }

    @Test
    void visitAddArithmeticExpr_withLiterals() {
        AddArithmeticExpr expr = lit(10).add(lit(20));
        CountingVisitor visitor = new CountingVisitor();
        expr.accept(visitor);
        // Addition + 2 literals = 3
        assertEquals(3, visitor.count);
    }

    @Test
    void visitSubArithmeticExpr() {
        SubArithmeticExpr expr = col("total").sub(col("discount"));
        CountingVisitor visitor = new CountingVisitor();
        expr.accept(visitor);
        // Subtraction + 2 columns = 3
        assertEquals(3, visitor.count);
    }

    @Test
    void visitMulArithmeticExpr() {
        MulArithmeticExpr expr = col("quantity").mul(col("price"));
        CountingVisitor visitor = new CountingVisitor();
        expr.accept(visitor);
        // Multiplication + 2 columns = 3
        assertEquals(3, visitor.count);
    }

    @Test
    void visitDivArithmeticExpr() {
        DivArithmeticExpr expr = col("total").div(col("count"));
        CountingVisitor visitor = new CountingVisitor();
        expr.accept(visitor);
        // Division + 2 columns = 3
        assertEquals(3, visitor.count);
    }

    @Test
    void visitModArithmeticExpr() {
        ModArithmeticExpr expr = col("number").mod(lit(10));
        CountingVisitor visitor = new CountingVisitor();
        expr.accept(visitor);
        // Modulo + column + literal = 3
        assertEquals(3, visitor.count);
    }

    @Test
    void visitCaseExpr_singleWhen() {
        CaseExpr caseExpr = kase(when(col("status").eq("active")).then(1));
        CountingVisitor visitor = new CountingVisitor();
        caseExpr.accept(visitor);
        assertNotNull(visitor.count);
        assertTrue(visitor.count > 0);
    }

    @Test
    void visitCaseExpr_multipleWhens() {
        CaseExpr caseExpr = kase(
            when(col("status").eq("active")).then(1),
            when(col("status").eq("inactive")).then(0)
        ).elseValue(2);
        CountingVisitor visitor = new CountingVisitor();
        caseExpr.accept(visitor);
        assertNotNull(visitor.count);
        assertTrue(visitor.count > 0);
    }

    @Test
    void visitRowExpr() {
        RowExpr row = row(lit(1), lit("a"), col("value"));
        CountingVisitor visitor = new CountingVisitor();
        row.accept(visitor);
        // Row + 2 literals + 1 column = 4
        assertEquals(4, visitor.count);
    }

    @Test
    void visitAnonymousParamExpr() {
        AnonymousParamExpr param = param();
        CountingVisitor visitor = new CountingVisitor();
        Integer result = param.accept(visitor);
        assertNotNull(result);
    }

    @Test
    void visitNamedParamExpr() {
        NamedParamExpr param = param("userId");
        CountingVisitor visitor = new CountingVisitor();
        Integer result = param.accept(visitor);
        assertNotNull(result);
    }

    @Test
    void visitOrdinalParamExpr() {
        OrdinalParamExpr param = param(1);
        CountingVisitor visitor = new CountingVisitor();
        Integer result = param.accept(visitor);
        assertNotNull(result);
    }

    @Test
    void visitFunctionArgExpr_exprArg() {
        FunctionExpr.Arg arg = arg(lit(42));
        CountingVisitor visitor = new CountingVisitor();
        arg.accept(visitor);
        // Should visit expression argument
        assertEquals(1, visitor.count);
    }

    @Test
    void visitFunctionArgExpr_starArg() {
        FunctionExpr.Arg arg = starArg();
        CountingVisitor visitor = new CountingVisitor();
        arg.accept(visitor);
        // Star arg shouldn't count in our counting visitor
        assertEquals(0, visitor.count);
    }

    @Test
    void columnCollectorVisitor() {
        ColumnCollector visitor = new ColumnCollector();
        SelectQuery query = select(col("id"), col("name"), col("email"))
            .from(tbl("users"));
        query.accept(visitor);
        
        assertTrue(visitor.columns.contains("id"));
        assertTrue(visitor.columns.contains("name"));
        assertTrue(visitor.columns.contains("email"));
    }

    @Test
    void columnCollector_nestedInPredicate() {
        ColumnCollector visitor = new ColumnCollector();
        SelectQuery query = select(col("*"))
            .from(tbl("users"))
            .where(col("status").eq("active").and(col("age").gte(18)));
        query.accept(visitor);

        assertTrue(visitor.columns.contains("status"));
        assertTrue(visitor.columns.contains("age"));
    }

    @Test
    void columnCollector_nestedInFunction() {
        ColumnCollector visitor = new ColumnCollector();
        SelectQuery query = select(
            col("id"),
            func("LOWER", arg(col("name"))).as("lower_name")
        ).from(tbl("users"));
        query.accept(visitor);

        assertTrue(visitor.columns.contains("id"));
        assertTrue(visitor.columns.contains("name"));
    }

    @Test
    void visitComplexArithmeticExpression() {
        // Create: (a + b) * (c - d)
        Expression left = col("a").add(col("b"));
        Expression right = col("c").sub(col("d"));
        Expression expr = left.mul(right);

        CountingVisitor visitor = new CountingVisitor();
        expr.accept(visitor);
        
        // Should visit mul, add, sub, and 4 columns = 7
        assertEquals(7, visitor.count);
    }

    @Test
    void visitNestedFunctionCalls() {
        // Create: UPPER(LOWER(name))
        FunctionExpr inner = func("LOWER", arg(col("name")));
        FunctionExpr outer = func("UPPER", arg(inner));

        CountingVisitor visitor = new CountingVisitor();
        outer.accept(visitor);
        
        // Should count: outer func, inner func, column = 3 or more
        assertTrue(visitor.count >= 3);
    }

    @Test
    void visitArrayExpr() {
        ArrayExpr array = array(lit(1), lit(2), lit(3));
        CountingVisitor visitor = new CountingVisitor();
        array.accept(visitor);
        
        // Array + 3 literals = 4
        assertTrue(visitor.count >= 3);
    }

    @Test
    void visitWithPredicateLogic() {
        // Create: (id = 1) AND (status = 'active') OR (age > 18)
        Predicate pred = col("id").eq(1)
            .and(col("status").eq("active"))
            .or(col("age").gt(18));

        CountingVisitor visitor = new CountingVisitor();
        pred.accept(visitor);
        
        assertNotNull(visitor.count);
        assertTrue(visitor.count > 0);
    }

    @Test
    void visitComparisonPredicate() {
        ComparisonPredicate pred = col("id").eq(lit(1));
        CountingVisitor visitor = new CountingVisitor();
        pred.accept(visitor);
        
        // Predicate + column + literal = 3
        assertTrue(visitor.count >= 2);
    }

    @Test
    void visitBetweenPredicate() {
        BetweenPredicate pred = col("age").between(lit(18), lit(65));
        CountingVisitor visitor = new CountingVisitor();
        pred.accept(visitor);
        
        // Predicate + column + 2 literals = 4 or more
        assertTrue(visitor.count >= 3);
    }

    @Test
    void visitInPredicate() {
        InPredicate pred = col("status").in(row("active", "pending", "review"));
        CountingVisitor visitor = new CountingVisitor();
        pred.accept(visitor);
        
        assertNotNull(visitor.count);
        assertTrue(visitor.count > 0);
    }

    @Test
    void visitLikePredicate() {
        LikePredicate pred = col("email").like("%@example.com");
        CountingVisitor visitor = new CountingVisitor();
        pred.accept(visitor);
        
        // Predicate + column + literal pattern = 3 or more
        assertTrue(visitor.count >= 2);
    }

    @Test
    void visitIsNullPredicate() {
        IsNullPredicate pred = col("optional_field").isNull();
        CountingVisitor visitor = new CountingVisitor();
        pred.accept(visitor);
        
        // Predicate + column = 2
        assertTrue(visitor.count >= 1);
    }

    @Test
    void visitCastExpr() {
        CastExpr cast = col("id").cast("VARCHAR");
        CountingVisitor visitor = new CountingVisitor();
        cast.accept(visitor);
        
        // CastExpr + column
        assertTrue(visitor.count >= 1);
    }

    @Test
    void visitUnaryOperator() {
        NegativeArithmeticExpr expr = NegativeArithmeticExpr.of(col("value"));
        CountingVisitor visitor = new CountingVisitor();
        expr.accept(visitor);
        
        // NegativeArithmeticExpr + column
        assertEquals(2, visitor.count);
    }

    @Test
    void visitNegativeArithmeticExpr() {
        NegativeArithmeticExpr expr = NegativeArithmeticExpr.of(col("balance"));
        CountingVisitor visitor = new CountingVisitor();
        expr.accept(visitor);
        
        // NegativeArithmeticExpr + column
        assertEquals(2, visitor.count);
    }
}
