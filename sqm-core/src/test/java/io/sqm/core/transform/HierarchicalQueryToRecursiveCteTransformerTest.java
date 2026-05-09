package io.sqm.core.transform;

import io.sqm.core.ColumnExpr;
import io.sqm.core.CompositeQuery;
import io.sqm.core.ExprSelectItem;
import io.sqm.core.OrderBy;
import io.sqm.core.SelectQuery;
import io.sqm.core.SetOperator;
import io.sqm.core.Table;
import io.sqm.core.WithQuery;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.hierarchy;
import static io.sqm.dsl.Dsl.prior;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.*;

class HierarchicalQueryToRecursiveCteTransformerTest {
    @Test
    void rewritesSimpleHierarchicalQueryToRecursiveCte() {
        var query = select(col("id"), col("parent_id"), col("level"))
            .from(tbl("categories"))
            .hierarchical(hierarchy(col("parent_id").isNull(), prior(col("id")).eq(col("parent_id")), false))
            .build();

        var transformed = new HierarchicalQueryToRecursiveCteTransformer().rewrite(query);

        assertTrue(transformed.recursive());
        assertEquals(1, transformed.ctes().size());
        var cte = transformed.ctes().getFirst();
        assertEquals("sqm_tree", cte.name().value());
        assertEquals(java.util.List.of("id", "parent_id", "level"), cte.columnAliases().stream().map(id -> id.value()).toList());

        var body = assertInstanceOf(CompositeQuery.class, cte.body());
        assertEquals(java.util.List.of(SetOperator.UNION_ALL), body.ops());
        assertEquals(2, body.terms().size());

        var anchor = assertInstanceOf(SelectQuery.class, body.terms().getFirst());
        var anchorTable = assertInstanceOf(Table.class, anchor.from());
        assertEquals("categories", anchorTable.name().value());
        assertEquals("sqm_child", anchorTable.alias().value());
        assertNotNull(anchor.where());

        var recursive = assertInstanceOf(SelectQuery.class, body.terms().get(1));
        assertEquals(1, recursive.joins().size());

        var outer = assertInstanceOf(SelectQuery.class, transformed.body());
        assertNull(outer.hierarchical());
        var firstOuterItem = assertInstanceOf(ExprSelectItem.class, outer.items().getFirst());
        var firstOuterColumn = assertInstanceOf(ColumnExpr.class, firstOuterItem.expr());
        assertEquals("sqm_tree", firstOuterColumn.tableAlias().value());
    }

    @Test
    void transformDispatchRewritesHierarchicalSelectQuery() {
        var query = select(col("id"))
            .from(tbl("categories"))
            .hierarchical(hierarchy(null, prior(col("id")).eq(col("parent_id")), false))
            .build();

        var transformed = new HierarchicalQueryToRecursiveCteTransformer().transform(query);

        assertInstanceOf(WithQuery.class, transformed);
    }

    @Test
    void rejectsSiblingOrderingBecauseItRequiresAdditionalTraversalSemantics() {
        var query = select(col("id"))
            .from(tbl("categories"))
            .hierarchical(hierarchy(null, prior(col("id")).eq(col("parent_id")), false, OrderBy.of(col("name").asc())))
            .build();

        var ex = assertThrows(
            HierarchicalQueryToRecursiveCteTransformer.UnsupportedRewriteException.class,
            () -> new HierarchicalQueryToRecursiveCteTransformer().rewrite(query)
        );
        assertTrue(ex.getMessage().contains("ORDER SIBLINGS BY"));
    }

    @Test
    void rejectsPlainSelectQueryForDirectRewrite() {
        var query = select(col("id")).from(tbl("categories")).build();

        var ex = assertThrows(
            HierarchicalQueryToRecursiveCteTransformer.UnsupportedRewriteException.class,
            () -> new HierarchicalQueryToRecursiveCteTransformer().rewrite(query)
        );
        assertTrue(ex.getMessage().contains("No hierarchical query"));
    }
}
