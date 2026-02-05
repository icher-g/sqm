package io.sqm.parser.ansi;


import io.sqm.core.*;
import io.sqm.parser.core.ParserException;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.Specs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for parsing arithmetic expressions in SQL queries.
 *
 * <p>These tests verify that the parser:
 * <ul>
 *     <li>Builds the correct {@link ArithmeticExpr} subtype for each operator.</li>
 *     <li>Respects operator precedence (multiplication/division/modulo before addition/subtraction).</li>
 *     <li>Respects parentheses.</li>
 *     <li>Parses unary negation into {@link NegativeArithmeticExpr}.</li>
 *     <li>Builds arithmetic expressions inside predicates (e.g. comparison in WHERE).</li>
 * </ul>
 * The tests focus on the shape of the parsed AST rather than rendering.</p>
 */
class ArithmeticParserTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Parses a query and returns the expression of the first SELECT item.
     *
     * @param sql SQL query string
     * @return the expression of the first select item
     */
    private Expression selectExpr(String sql) {
        return selectExpr(sql, new AnsiSpecs());
    }

    /**
     * Parses a query and returns the expression of the first SELECT item.
     *
     * @param sql SQL query string
     * @param specs specs to use.
     * @return the expression of the first select item
     */
    private Expression selectExpr(String sql, Specs specs) {
        Query q = parseQuery(sql, specs);

        return q.<Expression>matchQuery()
            .select(s -> {
                assertFalse(s.select().items().isEmpty(), "Expected at least one select item");
                var item = s.select().items().getFirst();
                assertInstanceOf(ExprSelectItem.class, item);
                return ((ExprSelectItem) item).expr();
            })
            .orElseThrow(() -> new AssertionError("Expected SelectQuery"));
    }

    // -------------------------------------------------------------------------
    // Basic arithmetic
    // -------------------------------------------------------------------------

    @Test
    void parses_simple_addition() {
        Expression expr = selectExpr("SELECT a + b FROM t");

        assertInstanceOf(AddArithmeticExpr.class, expr);
        AddArithmeticExpr add = (AddArithmeticExpr) expr;

        assertInstanceOf(io.sqm.core.ColumnExpr.class, add.lhs());
        assertEquals("a", ((io.sqm.core.ColumnExpr) add.lhs()).name());

        assertInstanceOf(io.sqm.core.ColumnExpr.class, add.rhs());
        assertEquals("b", ((io.sqm.core.ColumnExpr) add.rhs()).name());
    }

    @Test
    void parses_simple_multiplication() {
        Expression expr = selectExpr("SELECT a * b FROM t");

        assertInstanceOf(MulArithmeticExpr.class, expr);
        MulArithmeticExpr mul = (MulArithmeticExpr) expr;

        assertInstanceOf(io.sqm.core.ColumnExpr.class, mul.lhs());
        assertEquals("a", ((io.sqm.core.ColumnExpr) mul.lhs()).name());

        assertInstanceOf(io.sqm.core.ColumnExpr.class, mul.rhs());
        assertEquals("b", ((io.sqm.core.ColumnExpr) mul.rhs()).name());
    }

    // -------------------------------------------------------------------------
    // Precedence and parentheses
    // -------------------------------------------------------------------------

    @Test
    void multiplication_has_higher_precedence_than_addition() {
        // SELECT a + b * 2 FROM t
        Expression expr = selectExpr("SELECT a + b * 2 FROM t");

        // a + (b * 2)
        assertInstanceOf(AddArithmeticExpr.class, expr);
        AddArithmeticExpr add = (AddArithmeticExpr) expr;

        assertInstanceOf(io.sqm.core.ColumnExpr.class, add.lhs());
        assertEquals("a", ((io.sqm.core.ColumnExpr) add.lhs()).name());

        assertInstanceOf(MulArithmeticExpr.class, add.rhs());
        MulArithmeticExpr mul = (MulArithmeticExpr) add.rhs();

        assertInstanceOf(io.sqm.core.ColumnExpr.class, mul.lhs());
        assertEquals("b", ((io.sqm.core.ColumnExpr) mul.lhs()).name());

        assertInstanceOf(LiteralExpr.class, mul.rhs());
        assertEquals(2L, ((LiteralExpr) mul.rhs()).value());
    }

    @Test
    void parentheses_override_precedence() {
        // SELECT (a + b) * 2 FROM t
        Expression expr = selectExpr("SELECT (a + b) * 2 FROM t");

        // (a + b) * 2
        assertInstanceOf(MulArithmeticExpr.class, expr);
        MulArithmeticExpr mul = (MulArithmeticExpr) expr;

        assertInstanceOf(AddArithmeticExpr.class, mul.lhs());
        AddArithmeticExpr add = (AddArithmeticExpr) mul.lhs();

        assertInstanceOf(io.sqm.core.ColumnExpr.class, add.lhs());
        assertEquals("a", ((io.sqm.core.ColumnExpr) add.lhs()).name());

        assertInstanceOf(io.sqm.core.ColumnExpr.class, add.rhs());
        assertEquals("b", ((io.sqm.core.ColumnExpr) add.rhs()).name());

        assertInstanceOf(LiteralExpr.class, mul.rhs());
        assertEquals(2L, ((LiteralExpr) mul.rhs()).value());
    }

    @Test
    void subtraction_is_left_associative() {
        // SELECT a - b - c FROM t
        Expression expr = selectExpr("SELECT a - b - c FROM t");

        // (a - b) - c
        assertInstanceOf(SubArithmeticExpr.class, expr);
        SubArithmeticExpr outer = (SubArithmeticExpr) expr;

        assertInstanceOf(SubArithmeticExpr.class, outer.lhs());
        SubArithmeticExpr inner = (SubArithmeticExpr) outer.lhs();

        assertInstanceOf(io.sqm.core.ColumnExpr.class, inner.lhs());
        assertEquals("a", ((io.sqm.core.ColumnExpr) inner.lhs()).name());

        assertInstanceOf(io.sqm.core.ColumnExpr.class, inner.rhs());
        assertEquals("b", ((io.sqm.core.ColumnExpr) inner.rhs()).name());

        assertInstanceOf(io.sqm.core.ColumnExpr.class, outer.rhs());
        assertEquals("c", ((io.sqm.core.ColumnExpr) outer.rhs()).name());
    }

    @Test
    void div_and_mod_share_precedence_and_are_left_associative() {
        // Assuming grammar: factor (('*' | '/' | '%') factor)*  → left associative
        // SELECT a / b % c FROM t  → (a / b) % c
        Expression expr = selectExpr("SELECT a / b % c FROM t");

        assertInstanceOf(ModArithmeticExpr.class, expr);
        ModArithmeticExpr mod = (ModArithmeticExpr) expr;

        assertInstanceOf(DivArithmeticExpr.class, mod.lhs());
        DivArithmeticExpr div = (DivArithmeticExpr) mod.lhs();

        assertInstanceOf(io.sqm.core.ColumnExpr.class, div.lhs());
        assertEquals("a", ((io.sqm.core.ColumnExpr) div.lhs()).name());

        assertInstanceOf(io.sqm.core.ColumnExpr.class, div.rhs());
        assertEquals("b", ((io.sqm.core.ColumnExpr) div.rhs()).name());

        assertInstanceOf(io.sqm.core.ColumnExpr.class, mod.rhs());
        assertEquals("c", ((io.sqm.core.ColumnExpr) mod.rhs()).name());
    }

    // -------------------------------------------------------------------------
    // Unary negative
    // -------------------------------------------------------------------------

    @Test
    void parses_unary_negative() {
        // SELECT -a FROM t
        Expression expr = selectExpr("SELECT -a FROM t");

        assertInstanceOf(NegativeArithmeticExpr.class, expr);
        NegativeArithmeticExpr neg = (NegativeArithmeticExpr) expr;

        assertInstanceOf(io.sqm.core.ColumnExpr.class, neg.expr());
        assertEquals("a", ((io.sqm.core.ColumnExpr) neg.expr()).name());
    }

    @Test
    void parses_unary_negative_with_addition() {
        // SELECT -a + b FROM t  → (-a) + b
        Expression expr = selectExpr("SELECT -a + b FROM t");

        assertInstanceOf(AddArithmeticExpr.class, expr);
        AddArithmeticExpr add = (AddArithmeticExpr) expr;

        assertInstanceOf(NegativeArithmeticExpr.class, add.lhs());
        NegativeArithmeticExpr neg = (NegativeArithmeticExpr) add.lhs();

        assertInstanceOf(io.sqm.core.ColumnExpr.class, neg.expr());
        assertEquals("a", ((io.sqm.core.ColumnExpr) neg.expr()).name());

        assertInstanceOf(io.sqm.core.ColumnExpr.class, add.rhs());
        assertEquals("b", ((io.sqm.core.ColumnExpr) add.rhs()).name());
    }

    // -------------------------------------------------------------------------
    // Arithmetic inside predicates
    // -------------------------------------------------------------------------

    @Test
    void parses_arithmetic_in_where_comparison() {
        String sql = "SELECT * FROM t WHERE a + 1 = b - 2";

        Query q = parseQuery(sql);

        SelectQuery select = q.<SelectQuery>matchQuery()
            .select(s -> s)
            .orElseThrow(() -> new AssertionError("Expected SelectQuery"));

        assertNotNull(select.where(), "Expected WHERE clause");
        var predicate = select.where();
        assertInstanceOf(ComparisonPredicate.class, predicate);

        ComparisonPredicate cmp = (ComparisonPredicate) predicate;

        assertInstanceOf(AddArithmeticExpr.class, cmp.lhs());
        AddArithmeticExpr leftAdd = (AddArithmeticExpr) cmp.lhs();

        assertInstanceOf(io.sqm.core.ColumnExpr.class, leftAdd.lhs());
        assertEquals("a", ((io.sqm.core.ColumnExpr) leftAdd.lhs()).name());

        assertInstanceOf(LiteralExpr.class, leftAdd.rhs());
        assertEquals(1L, ((LiteralExpr) leftAdd.rhs()).value());

        assertInstanceOf(SubArithmeticExpr.class, cmp.rhs());
        SubArithmeticExpr rightSub = (SubArithmeticExpr) cmp.rhs();

        assertInstanceOf(io.sqm.core.ColumnExpr.class, rightSub.lhs());
        assertEquals("b", ((io.sqm.core.ColumnExpr) rightSub.lhs()).name());

        assertInstanceOf(LiteralExpr.class, rightSub.rhs());
        assertEquals(2L, ((LiteralExpr) rightSub.rhs()).value());
    }

    @Test
    void power_is_left_associative_in_postgres() {
        // 2 ^ 3 ^ 3  -> (2 ^ 3) ^ 3
        var expr = selectExpr("SELECT 2 ^ 3 ^ 3", new TestSpecs());
        assertInstanceOf(PowerArithmeticExpr.class, expr);

        var top = (PowerArithmeticExpr) expr;
        assertInstanceOf(PowerArithmeticExpr.class, top.lhs());
    }

    @Test
    void power_has_higher_precedence_than_multiplicative() {
        // 2 * 3 ^ 2  -> 2 * (3 ^ 2)
        var expr = selectExpr("SELECT 2 * 3 ^ 2", new TestSpecs());
        var mul = (MultiplicativeArithmeticExpr) expr;
        assertInstanceOf(PowerArithmeticExpr.class, mul.rhs());
    }

    // -------------------------------------------------------------------------
    // Parser hook
    // -------------------------------------------------------------------------

    /**
     * Helper to adapt tests to the actual parser API.
     * <p>
     * Replace the body with your real entry point, for example:
     * {@code return SqlParser.parseQuery(sql); }
     */
    private Query parseQuery(String sql) {
        return parseQuery(sql, new AnsiSpecs());
    }

    /**
     * Helper to adapt tests to the actual parser API.
     * <p>
     * Replace the body with your real entry point, for example:
     * {@code return SqlParser.parseQuery(sql); }
     */
    private Query parseQuery(String sql, Specs specs) {
        var ctx = ParseContext.of(specs);
        var res = ctx.parse(Query.class, sql);
        if (res.isError()) {
            throw new ParserException(res.errorMessage(), 0);
        }
        return res.value();
    }
}

