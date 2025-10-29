package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.JoinParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JoinParserTest {

    private final ParseContext ctx = ParseContext.of(new AnsiSpecs());

    @Test
    @DisplayName("INNER JOIN with alias and simple ON equality")
    void inner_join_with_on_eq() {
        var p = new JoinParser();
        ParseResult<Join> r = p.parse("JOIN products p ON p.category_id = c.id", ctx);

        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        Join j = r.value();
        Assertions.assertInstanceOf(OnJoin.class, j);
        OnJoin tj = (OnJoin) j;
        Assertions.assertEquals(JoinKind.INNER, tj.kind());
        Assertions.assertEquals("products", tj.right().asTable().name());
        Assertions.assertEquals("p", tj.right().alias());

        Predicate on = tj.on();
        Assertions.assertNotNull(on, "ON filter expected");
    }

    @Test
    @DisplayName("LEFT OUTER JOIN with AS alias and compound ON")
    void left_outer_with_compound_on() {
        var p = new JoinParser();
        var r = p.parse("LEFT OUTER JOIN warehouses AS w ON w.product_id = p.id AND w.stock > 0", ctx);

        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        OnJoin j = (OnJoin) r.value();
        Assertions.assertEquals(JoinKind.LEFT, j.kind());
        Assertions.assertEquals("warehouses", j.right().asTable().name());
        Assertions.assertEquals("w", j.right().alias());

        Assertions.assertInstanceOf(AndPredicate.class, j.on(), "Expected a composite AND");
        var and = (AndPredicate) j.on();
        Assertions.assertInstanceOf(ComparisonPredicate.class, and.lhs());
        Assertions.assertInstanceOf(ComparisonPredicate.class, and.rhs());
    }

    @Test
    @DisplayName("CROSS JOIN with alias (no ON)")
    void cross_join_without_on() {
        var p = new JoinParser();
        var r = p.parse("CROSS JOIN regions r", ctx);

        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        CrossJoin j = (CrossJoin) r.value();
        Assertions.assertEquals("regions", j.right().asTable().name());
        Assertions.assertEquals("r", j.right().alias());
    }

    @Test
    @DisplayName("Qualified table name schema.table and implicit INNER")
    void qualified_table_implicit_inner() {
        var p = new JoinParser();
        var r = p.parse("JOIN sales.products AS sp ON sp.id = p.id", ctx);

        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        OnJoin j = (OnJoin) r.value();
        Assertions.assertEquals(JoinKind.INNER, j.kind());
        Assertions.assertEquals("sales", j.right().asTable().schema());
        Assertions.assertEquals("products", j.right().asTable().name());
        Assertions.assertEquals("sp", j.right().alias());
    }

    @Test
    @DisplayName("RIGHT JOIN without OUTER and simple ON")
    void right_join() {
        var p = new JoinParser();
        var r = p.parse("RIGHT JOIN t2 ON t2.k = t1.k", ctx);

        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        OnJoin j = (OnJoin) r.value();
        Assertions.assertEquals(JoinKind.RIGHT, j.kind());
        Assertions.assertEquals("t2", j.right().asTable().name());
        Assertions.assertNull(j.right().asTable().schema());
        Assertions.assertNotNull(j.on());
    }
}