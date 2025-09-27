package io.cherlabs.sqlmodel.parser;

import io.cherlabs.sqlmodel.core.*;
import io.cherlabs.sqlmodel.core.views.Tables;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JoinSpecParserTest {

    @Test
    @DisplayName("INNER JOIN with alias and simple ON equality")
    void inner_join_with_on_eq() {
        var p = new JoinSpecParser();
        ParseResult<Join> r = p.parse("JOIN products p ON p.category_id = c.id");

        assertTrue(r.ok(), () -> "problems: " + r.problems());
        Join j = r.value();
        assertInstanceOf(TableJoin.class, j);
        TableJoin tj = (TableJoin) j;
        assertEquals(Join.JoinType.Inner, tj.joinType());
        assertEquals(Optional.of("products"), Tables.name(tj.table()));
        assertEquals(Optional.of("p"), Tables.alias(tj.table()));

        Filter on = tj.on();
        assertNotNull(on, "ON filter expected");
    }

    @Test
    @DisplayName("LEFT OUTER JOIN with AS alias and compound ON")
    void left_outer_with_compound_on() {
        var p = new JoinSpecParser();
        var r = p.parse("LEFT OUTER JOIN warehouses AS w ON w.product_id = p.id AND w.stock > 0");

        assertTrue(r.ok(), () -> "problems: " + r.problems());
        TableJoin j = (TableJoin) r.value();
        assertEquals(Join.JoinType.Left, j.joinType());
        assertEquals(Optional.of("warehouses"), Tables.name(j.table()));
        assertEquals(Optional.of("w"), Tables.alias(j.table()));

        assertInstanceOf(CompositeFilter.class, j.on(), "Expected a composite AND");
        var and = (CompositeFilter) j.on();
        assertEquals(CompositeFilter.Operator.And, and.operator());
        assertEquals(2, and.filters().size(), "AND should contain two predicates");
    }

    @Test
    @DisplayName("CROSS JOIN with alias (no ON)")
    void cross_join_without_on() {
        var p = new JoinSpecParser();
        var r = p.parse("CROSS JOIN regions r");

        assertTrue(r.ok(), () -> "problems: " + r.problems());
        TableJoin j = (TableJoin) r.value();
        assertEquals(Join.JoinType.Cross, j.joinType());
        assertEquals(Optional.of("regions"), Tables.name(j.table()));
        assertEquals(Optional.of("r"), Tables.alias(j.table()));
        assertNull(j.on(), "CROSS JOIN should not have ON filter");
    }

    @Test
    @DisplayName("Qualified table name schema.table and implicit INNER")
    void qualified_table_implicit_inner() {
        var p = new JoinSpecParser();
        var r = p.parse("JOIN sales.products AS sp ON sp.id = p.id");

        assertTrue(r.ok(), () -> "problems: " + r.problems());
        TableJoin j = (TableJoin) r.value();
        assertEquals(Join.JoinType.Inner, j.joinType());
        assertEquals(Optional.of("sales"), Tables.schema(j.table()));
        assertEquals(Optional.of("products"), Tables.name(j.table()));
        assertEquals(Optional.of("sp"), Tables.alias(j.table()));
    }

    @Test
    @DisplayName("RIGHT JOIN without OUTER and simple ON")
    void right_join() {
        var p = new JoinSpecParser();
        var r = p.parse("RIGHT JOIN t2 ON t2.k = t1.k");

        assertTrue(r.ok(), () -> "problems: " + r.problems());
        TableJoin j = (TableJoin) r.value();
        assertEquals(Join.JoinType.Right, j.joinType());
        assertEquals(Optional.of("t2"), Tables.name(j.table()));
        assertEquals(Optional.empty(), Tables.schema(j.table()));
        assertNotNull(j.on());
    }

    @Test
    @DisplayName("JOIN without ON is allowed (except when ON is required by your policy)")
    void allow_join_without_on() {
        var p = new JoinSpecParser();
        var r = p.parse("JOIN t a");
        assertTrue(r.ok(), () -> "problems: " + r.problems());
        TableJoin j = (TableJoin) r.value();
        assertEquals(Join.JoinType.Inner, j.joinType());
        assertEquals(Optional.of("t"), Tables.name(j.table()));
        assertEquals(Optional.of("a"), Tables.alias(j.table()));
        assertNull(j.on());
    }
}