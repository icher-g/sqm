package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.QueryParser;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link BinaryOperatorExpr} / {@link UnaryOperatorExpr} parsing in query context.
 * <p>
 * These tests ensure operator expressions can appear in SELECT lists and in WHERE predicates
 * (via {@link UnaryPredicate} fallback) without introducing dialect-specific decisions in the ANSI parser.
 */
class OperatorExprIntegrationTest {

    private ParseContext ctx;
    private QueryParser queryParser;

    @BeforeEach
    void setUp() {
        this.ctx = ParseContext.of(new AnsiSpecs());
        this.queryParser = new QueryParser();
    }

    private Query parseOk(String sql) {
        var res = ctx.parse(queryParser, Cursor.of(sql));
        assertTrue(res.ok(), () -> "Expected OK, got: " + res);
        return res.value();
    }

    private void parseErr(String sql) {
        var res = ctx.parse(queryParser, Cursor.of(sql));
        assertFalse(res.ok(), () -> "Expected error, got OK with: " + res.value());
    }

    @Test
    void parses_unary_operator_in_select_item() {
        var q = (SelectQuery) parseOk("SELECT ~mask AS m FROM t");

        assertEquals(1, q.items().size());
        assertInstanceOf(ExprSelectItem.class, q.items().getFirst());

        var item = (ExprSelectItem) q.items().getFirst();
        assertInstanceOf(UnaryOperatorExpr.class, item.expr());

        var u = (UnaryOperatorExpr) item.expr();
        assertEquals("~", u.operator());
        assertInstanceOf(ColumnExpr.class, u.expr());
        assertEquals("mask", ((ColumnExpr) u.expr()).name());
    }

    @Test
    void parses_binary_operator_in_select_item() {
        var q = (SelectQuery) parseOk("SELECT payload ->> 'id' AS id FROM t");

        var item = (ExprSelectItem) q.items().getFirst();
        assertInstanceOf(BinaryOperatorExpr.class, item.expr());

        var b = (BinaryOperatorExpr) item.expr();
        assertEquals("->>", b.operator());
        assertInstanceOf(ColumnExpr.class, b.left());
        assertEquals("payload", ((ColumnExpr) b.left()).name());
        assertInstanceOf(LiteralExpr.class, b.right());
        assertEquals("id", ((LiteralExpr) b.right()).value());
    }

    @Test
    void parses_binary_operator_in_where_as_unary_predicate() {
        var q = (SelectQuery) parseOk("SELECT * FROM t WHERE data @> CAST('{}' AS jsonb)");

        assertNotNull(q.where());
        assertInstanceOf(UnaryPredicate.class, q.where());

        var p = (UnaryPredicate) q.where();
        assertInstanceOf(BinaryOperatorExpr.class, p.expr());

        var b = (BinaryOperatorExpr) p.expr();
        assertEquals("@>", b.operator());
        assertInstanceOf(ColumnExpr.class, b.left());
        assertEquals("data", ((ColumnExpr) b.left()).name());

        assertInstanceOf(CastExpr.class, b.right());
        var c = (CastExpr) b.right();
        assertEquals("jsonb", c.type().qualifiedName().getFirst());
        assertInstanceOf(LiteralExpr.class, c.expr());
        assertEquals("{}", ((LiteralExpr) c.expr()).value());
    }

    @Test
    void fails_parsing_array_which_is_not_supported() {
        parseErr("SELECT * FROM t WHERE tags && ARRAY['a','b']");
    }

    @Test
    void fails_when_binary_operator_missing_rhs_in_where() {
        parseErr("SELECT * FROM t WHERE data @>");
    }

    @Test
    void fails_when_unary_operator_missing_operand_in_select() {
        parseErr("SELECT ~ FROM t");
    }
}
