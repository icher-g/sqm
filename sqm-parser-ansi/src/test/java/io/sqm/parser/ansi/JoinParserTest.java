package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.JoinParser;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.IdentifierQuoting;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JoinParserTest {

    private final ParseContext ctx = ParseContext.of(new AnsiSpecs());
    private final JoinParser parser = new JoinParser();
    private final IdentifierQuoting quoting = IdentifierQuoting.of('"');

    private ParseResult<? extends Join> parse(String sql) {
        return ctx.parse(parser, Cursor.of(sql, quoting));
    }

    @Test
    @DisplayName("INNER JOIN with alias and simple ON equality")
    void inner_join_with_on_eq() {
        var r = parse("JOIN products p ON p.category_id = c.id");

        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        Join j = r.value();
        Assertions.assertInstanceOf(OnJoin.class, j);
        OnJoin tj = (OnJoin) j;
        Assertions.assertEquals(JoinKind.INNER, tj.kind());
        Assertions.assertEquals("products", tj.right().matchTableRef().table(t -> t.name()).orElse(null));
        Assertions.assertEquals("p", tj.right().matchTableRef().table(t -> t.alias()).orElse(null));

        Predicate on = tj.on();
        Assertions.assertNotNull(on, "ON filter expected");
    }

    @Test
    @DisplayName("LEFT OUTER JOIN with AS alias and compound ON")
    void left_outer_with_compound_on() {
        var r = parse("LEFT OUTER JOIN warehouses AS w ON w.product_id = p.id AND w.stock > 0");

        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        OnJoin j = (OnJoin) r.value();
        Assertions.assertEquals(JoinKind.LEFT, j.kind());
        Assertions.assertEquals("warehouses", j.right().matchTableRef().table(t -> t.name()).orElse(null));
        Assertions.assertEquals("w", j.right().matchTableRef().table(t -> t.alias()).orElse(null));

        Assertions.assertInstanceOf(AndPredicate.class, j.on(), "Expected a composite AND");
        var and = (AndPredicate) j.on();
        Assertions.assertInstanceOf(ComparisonPredicate.class, and.lhs());
        Assertions.assertInstanceOf(ComparisonPredicate.class, and.rhs());
    }

    @Test
    @DisplayName("CROSS JOIN with alias (no ON)")
    void cross_join_without_on() {
        var r = parse("CROSS JOIN regions r");

        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        CrossJoin j = (CrossJoin) r.value();
        Assertions.assertEquals("regions", j.right().matchTableRef().table(t -> t.name()).orElse(null));
        Assertions.assertEquals("r", j.right().matchTableRef().table(t -> t.alias()).orElse(null));
    }

    @Test
    @DisplayName("Qualified table name schema.table and implicit INNER")
    void qualified_table_implicit_inner() {
        var r = parse("JOIN sales.products AS sp ON sp.id = p.id");

        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        OnJoin j = (OnJoin) r.value();
        Assertions.assertEquals(JoinKind.INNER, j.kind());
        Assertions.assertEquals("sales", j.right().matchTableRef().table(t -> t.schema()).orElse(null));
        Assertions.assertEquals("products", j.right().matchTableRef().table(t -> t.name()).orElse(null));
        Assertions.assertEquals("sp", j.right().matchTableRef().table(t -> t.alias()).orElse(null));
    }

    @Test
    @DisplayName("RIGHT JOIN without OUTER and simple ON")
    void right_join() {
        var r = parse("RIGHT JOIN t2 ON t2.k = t1.k");

        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        OnJoin j = (OnJoin) r.value();
        Assertions.assertEquals(JoinKind.RIGHT, j.kind());
        Assertions.assertEquals("t2", j.right().matchTableRef().table(t -> t.name()).orElse(null));
        Assertions.assertNull(j.right().matchTableRef().table(t -> t.schema()).orElse(null));
        Assertions.assertNotNull(j.on());
    }
}