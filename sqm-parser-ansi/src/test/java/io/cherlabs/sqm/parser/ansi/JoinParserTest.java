package io.cherlabs.sqm.parser.ansi;

import io.cherlabs.sqm.core.CompositeFilter;
import io.cherlabs.sqm.core.Filter;
import io.cherlabs.sqm.core.Join;
import io.cherlabs.sqm.core.TableJoin;
import io.cherlabs.sqm.core.views.Tables;
import io.cherlabs.sqm.parser.JoinParser;
import io.cherlabs.sqm.parser.spi.ParseContext;
import io.cherlabs.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

class JoinParserTest {

    private final ParseContext ctx = ParseContext.of(new AnsiSpecs());

    @Test
    @DisplayName("INNER JOIN with alias and simple ON equality")
    void inner_join_with_on_eq() {
        var p = new JoinParser();
        ParseResult<Join> r = p.parse("JOIN products p ON p.category_id = c.id", ctx);

        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        Join j = r.value();
        Assertions.assertInstanceOf(TableJoin.class, j);
        TableJoin tj = (TableJoin) j;
        Assertions.assertEquals(Join.JoinType.Inner, tj.joinType());
        Assertions.assertEquals(Optional.of("products"), Tables.name(tj.table()));
        Assertions.assertEquals(Optional.of("p"), Tables.alias(tj.table()));

        Filter on = tj.on();
        Assertions.assertNotNull(on, "ON filter expected");
    }

    @Test
    @DisplayName("LEFT OUTER JOIN with AS alias and compound ON")
    void left_outer_with_compound_on() {
        var p = new JoinParser();
        var r = p.parse("LEFT OUTER JOIN warehouses AS w ON w.product_id = p.id AND w.stock > 0", ctx);

        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        TableJoin j = (TableJoin) r.value();
        Assertions.assertEquals(Join.JoinType.Left, j.joinType());
        Assertions.assertEquals(Optional.of("warehouses"), Tables.name(j.table()));
        Assertions.assertEquals(Optional.of("w"), Tables.alias(j.table()));

        Assertions.assertInstanceOf(CompositeFilter.class, j.on(), "Expected a composite AND");
        var and = (CompositeFilter) j.on();
        Assertions.assertEquals(CompositeFilter.Operator.And, and.op());
        Assertions.assertEquals(2, and.filters().size(), "AND should contain two predicates");
    }

    @Test
    @DisplayName("CROSS JOIN with alias (no ON)")
    void cross_join_without_on() {
        var p = new JoinParser();
        var r = p.parse("CROSS JOIN regions r", ctx);

        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        TableJoin j = (TableJoin) r.value();
        Assertions.assertEquals(Join.JoinType.Cross, j.joinType());
        Assertions.assertEquals(Optional.of("regions"), Tables.name(j.table()));
        Assertions.assertEquals(Optional.of("r"), Tables.alias(j.table()));
        Assertions.assertNull(j.on(), "CROSS JOIN should not have ON filter");
    }

    @Test
    @DisplayName("Qualified table name schema.table and implicit INNER")
    void qualified_table_implicit_inner() {
        var p = new JoinParser();
        var r = p.parse("JOIN sales.products AS sp ON sp.id = p.id", ctx);

        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        TableJoin j = (TableJoin) r.value();
        Assertions.assertEquals(Join.JoinType.Inner, j.joinType());
        Assertions.assertEquals(Optional.of("sales"), Tables.schema(j.table()));
        Assertions.assertEquals(Optional.of("products"), Tables.name(j.table()));
        Assertions.assertEquals(Optional.of("sp"), Tables.alias(j.table()));
    }

    @Test
    @DisplayName("RIGHT JOIN without OUTER and simple ON")
    void right_join() {
        var p = new JoinParser();
        var r = p.parse("RIGHT JOIN t2 ON t2.k = t1.k", ctx);

        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        TableJoin j = (TableJoin) r.value();
        Assertions.assertEquals(Join.JoinType.Right, j.joinType());
        Assertions.assertEquals(Optional.of("t2"), Tables.name(j.table()));
        Assertions.assertEquals(Optional.empty(), Tables.schema(j.table()));
        Assertions.assertNotNull(j.on());
    }

    @Test
    @DisplayName("JOIN without ON is allowed (except when ON is required by your policy)")
    void allow_join_without_on() {
        var p = new JoinParser();
        var r = p.parse("JOIN t a", ctx);
        Assertions.assertTrue(r.ok(), () -> "problems: " + r.problems());
        TableJoin j = (TableJoin) r.value();
        Assertions.assertEquals(Join.JoinType.Inner, j.joinType());
        Assertions.assertEquals(Optional.of("t"), Tables.name(j.table()));
        Assertions.assertEquals(Optional.of("a"), Tables.alias(j.table()));
        Assertions.assertNull(j.on());
    }
}