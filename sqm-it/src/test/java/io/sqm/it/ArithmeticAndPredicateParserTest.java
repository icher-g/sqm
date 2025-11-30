package io.sqm.it;

import io.sqm.core.Expression;
import io.sqm.core.Node;
import io.sqm.core.Predicate;
import io.sqm.core.Query;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArithmeticAndPredicateParserTest {

    private final ParseContext parseContext = ParseContext.of(new AnsiSpecs());
    private final RenderContext renderContext = RenderContext.of(new AnsiDialect());

    private Expression parseExpression(String sql) {
        return parseContext.parse(Expression.class, sql).value();
    }

    private Predicate parsePredicate(String sql) {
        return parseContext.parse(Predicate.class, sql).value();
    }

    private Query parseQuery(String sql) {
        return parseContext.parse(Query.class, sql).value();
    }

    private String render(Node node) {
        return renderContext.render(node).sql();
    }

    // ----------------------------------------------------------------------
    //  Arithmetic-only tests
    // ----------------------------------------------------------------------

    @Test
    void complexArithmetic_precedenceWithParentheses() {
        String exprSql = "(a + b) * 2";
        Expression expr = parseExpression(exprSql);
        String rendered = render(expr);

        assertEquals(exprSql, rendered);
    }

    @Test
    void complexArithmetic_nestedParenthesesAndMixedOps() {
        String exprSql = "(a + b) * (c - d) / 2";
        Expression expr = parseExpression(exprSql);
        String rendered = render(expr);

        assertEquals(exprSql, rendered);
    }

    @Test
    void complexArithmetic_unaryMinusAndMultiplicativeChain() {
        String exprSql = "-(a + 1) * (b - 2) / 3";
        Expression expr = parseExpression(exprSql);
        String rendered = render(expr);

        assertEquals(exprSql, rendered);
    }

    @Test
    void complexArithmetic_multipleOperatorsSamePrecedence() {
        String exprSql = "a * b / c % 2";
        Expression expr = parseExpression(exprSql);
        String rendered = render(expr);

        assertEquals("MOD(a * b / c, 2)", rendered);
    }

    // ----------------------------------------------------------------------
    //  Predicate-only tests
    // ----------------------------------------------------------------------

    @Test
    void predicate_comparisonWithArithmeticOnBothSides() {
        String predSql = "a + b * 2 > c - 3";
        Predicate p = parsePredicate(predSql);
        String rendered = render(p);

        assertEquals(predSql, rendered);
    }

    @Test
    void predicate_betweenAndInWithArithmetic() {
        String predSql = "a + b * 2 BETWEEN 10 AND c - 3 AND d NOT IN (1, 2, 3)";
        Predicate p = parsePredicate(predSql);
        String rendered = render(p);

        assertEquals(predSql, rendered);
    }

    @Test
    void predicate_andOrPrecedenceWithGrouping() {
        String predSql = "(a > 1 AND b < 2) OR c IS NOT NULL";
        Predicate p = parsePredicate(predSql);
        String rendered = render(p);

        assertEquals(predSql, rendered);
    }

    @Test
    void predicate_notWithGroupedPredicateAndArithmetic() {
        String predSql = "NOT (a + b * 2 > 10)";
        Predicate p = parsePredicate(predSql);
        String rendered = render(p);

        assertEquals(predSql, rendered);
    }

    @Test
    void predicate_existsWithCorrelatedArithmetic() {
        String predSql = """
            EXISTS (
              SELECT 1
              FROM t2
              WHERE t2.x = t1.x + 1 AND t2.y > t1.y * 2
            )""".trim();
        Predicate p = parsePredicate(predSql);
        String rendered = render(p);

        assertEquals(predSql, rendered);
    }

    // ----------------------------------------------------------------------
    //  Combined (SELECT + WHERE) tests
    // ----------------------------------------------------------------------

    @Test
    void selectWithComplexArithmeticInSelectListAndWhere() {
        String sql = """
            SELECT (a + b) * 2 AS total_ab, c - d / 3 AS adjusted_c
            FROM t
            WHERE a + b * 2 > 10 AND (c IS NOT NULL OR d BETWEEN 1 AND 5)
            """.trim();

        Query q = parseQuery(sql);
        String rendered = render(q);

        assertEquals(sql, rendered);
    }

    @Test
    void selectWithArithmeticInPredicate_andInClause() {
        String sql = """
            SELECT *
            FROM t
            WHERE a + b * 2 IN (10, 20, 30) AND c - 1 NOT IN (
              SELECT x
              FROM t2
              WHERE x > 100
            )
            """.trim();

        Query q = parseQuery(sql);
        String rendered = render(q);

        assertEquals(sql, rendered);
    }

    @Test
    void selectWithNestedArithmeticAndOrPredicates() {
        String sql = """
            SELECT *
            FROM t
            WHERE ((a + b) * 2 > c - 3 AND d IS NOT NULL) OR (e / 2 < f + 1 AND g LIKE 'TEST%')
            """.trim();

        Query q = parseQuery(sql);
        String rendered = render(q);

        assertEquals(sql, rendered);
    }

    @Test
    void selectWithNotAndExistsAndArithmetic() {
        String sql = """
            SELECT *
            FROM t1
            WHERE NOT EXISTS (
              SELECT 1
              FROM t2
              WHERE t2.id = t1.id AND t2.value + 10 >= t1.threshold
            )
            """.trim();

        Query q = parseQuery(sql);
        String rendered = render(q);

        assertEquals(sql, rendered);
    }
}
