package io.sqm.parser.oracle;

import io.sqm.core.ComparisonPredicate;
import io.sqm.core.ExprSelectItem;
import io.sqm.core.HierarchicalQueryClause;
import io.sqm.core.IsNullPredicate;
import io.sqm.core.PriorExpr;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.parser.oracle.spi.OracleSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class HierarchicalQueryParserTest {
    private final ParseContext ctx = ParseContext.of(new OracleSpecs());

    @Test
    void parsesStartWithConnectByNoCycleAndOrderSiblingsBy() {
        var result = ctx.parse(Query.class, """
            SELECT id, parent_id, LEVEL
            FROM categories
            START WITH parent_id IS NULL
            CONNECT BY NOCYCLE PRIOR id = parent_id
            ORDER SIBLINGS BY name
            """);

        assertTrue(result.ok(), result.errorMessage());
        var select = assertInstanceOf(SelectQuery.class, result.value());
        assertNotNull(select.hierarchical());
        var hierarchy = select.hierarchical();
        assertInstanceOf(IsNullPredicate.class, hierarchy.startWith());
        assertTrue(hierarchy.noCycle());
        assertEquals(1, hierarchy.orderSiblingsBy().items().size());

        var connect = assertInstanceOf(ComparisonPredicate.class, hierarchy.connectBy());
        assertInstanceOf(PriorExpr.class, connect.lhs());
        var level = assertInstanceOf(ExprSelectItem.class, select.items().get(2));
        assertEquals("LEVEL", level.expr().matchExpression().column(c -> c.name().value()).orElse("missing"));
    }

    @Test
    void parsesConnectByBeforeStartWith() {
        var result = ctx.parse(Query.class, """
            SELECT id
            FROM categories
            CONNECT BY PRIOR id = parent_id
            START WITH parent_id IS NULL
            """);

        assertTrue(result.ok(), result.errorMessage());
        var select = assertInstanceOf(SelectQuery.class, result.value());
        assertNotNull(select.hierarchical().startWith());
        assertFalse(select.hierarchical().noCycle());
    }

    @Test
    void parsesHierarchicalQueryClauseDirectly() {
        var result = ctx.parse(HierarchicalQueryClause.class, """
            START WITH parent_id IS NULL
            CONNECT BY PRIOR id = parent_id
            ORDER SIBLINGS BY name
            """);

        assertTrue(result.ok(), result.errorMessage());
        var hierarchy = result.value();
        assertInstanceOf(IsNullPredicate.class, hierarchy.startWith());
        assertNotNull(hierarchy.connectBy());
        assertEquals(1, hierarchy.orderSiblingsBy().items().size());
    }

    @Test
    void rejectsStartWithWithoutConnectBy() {
        var result = ctx.parse(Query.class, "SELECT id FROM categories START WITH parent_id IS NULL");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Expected CONNECT BY"));
    }

    @Test
    void rejectsInvalidOrderSiblingsItem() {
        var result = ctx.parse(HierarchicalQueryClause.class, """
            CONNECT BY PRIOR id = parent_id
            ORDER SIBLINGS BY name,
            """);

        assertTrue(result.isError());
    }
}
