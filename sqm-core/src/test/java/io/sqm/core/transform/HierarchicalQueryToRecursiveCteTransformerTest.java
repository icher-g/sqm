package io.sqm.core.transform;

import io.sqm.core.ColumnExpr;
import io.sqm.core.CompositeQuery;
import io.sqm.core.ExprSelectItem;
import io.sqm.core.Expression;
import io.sqm.core.Identifier;
import io.sqm.core.Join;
import io.sqm.core.LockMode;
import io.sqm.core.OrderBy;
import io.sqm.core.OverSpec;
import io.sqm.core.PartitionBy;
import io.sqm.core.QueryTable;
import io.sqm.core.QuoteStyle;
import io.sqm.core.SelectQuery;
import io.sqm.core.SelectQueryBuilder;
import io.sqm.core.SetOperator;
import io.sqm.core.Table;
import io.sqm.core.WithQuery;
import io.sqm.core.WindowDef;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.func;
import static io.sqm.dsl.Dsl.hierarchy;
import static io.sqm.dsl.Dsl.prior;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.star;
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
    void transformDispatchPreservesPlainSelectQuery() {
        var query = select(col("id")).from(tbl("categories")).build();

        var transformed = new HierarchicalQueryToRecursiveCteTransformer().transform(query);

        assertSame(query, transformed);
    }

    @Test
    void rewritesWithoutStartWithAndWithReversedPriorSide() {
        var query = select(col("id").as("node_id"), col("level").as("depth"))
            .from(tbl("categories").as("c"))
            .hierarchical(hierarchy(null, col("c", "parent_id").eq(prior(col("c", "id"))), false))
            .build();

        var transformed = new HierarchicalQueryToRecursiveCteTransformer().rewrite(query);
        var cte = transformed.ctes().getFirst();
        var body = assertInstanceOf(CompositeQuery.class, cte.body());
        var anchor = assertInstanceOf(SelectQuery.class, body.terms().getFirst());
        var recursive = assertInstanceOf(SelectQuery.class, body.terms().get(1));
        var outer = assertInstanceOf(SelectQuery.class, transformed.body());

        assertNull(anchor.where());
        assertEquals(java.util.List.of("node_id", "depth"), cte.columnAliases().stream().map(Identifier::value).toList());
        assertEquals(1, recursive.joins().size());
        var outerItem = assertInstanceOf(ExprSelectItem.class, outer.items().getFirst());
        assertEquals("node_id", outerItem.alias().value());
    }

    @Test
    void rewritesQuotedSourceAliases() {
        var sourceAlias = Identifier.of("C", QuoteStyle.DOUBLE_QUOTE);
        var query = select(ColumnExpr.of(sourceAlias, Identifier.of("id")))
            .from(tbl("categories").as(sourceAlias))
            .hierarchical(hierarchy(
                null,
                prior(ColumnExpr.of(sourceAlias, Identifier.of("id")))
                    .eq(ColumnExpr.of(sourceAlias, Identifier.of("parent_id"))),
                false))
            .build();

        var transformed = new HierarchicalQueryToRecursiveCteTransformer().rewrite(query);

        assertEquals(1, transformed.ctes().getFirst().columnAliases().size());
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
    void rejectsUnsupportedSelectShapes() {
        assertUnsupported(
            select(col("id"))
                .from(QueryTable.of(select(col("id")).from(tbl("categories")).build()))
                .hierarchical(hierarchy(null, prior(col("id")).eq(col("parent_id")), false))
                .build(),
            "single base table"
        );
        assertUnsupported(
            baseQuery().join(Join.cross(tbl("other"))).build(),
            "does not support joins"
        );
        assertUnsupported(
            baseQuery().where(col("active").eq(true)).build(),
            "SELECT WHERE"
        );
        assertUnsupported(
            baseQuery().groupBy(col("id")).build(),
            "grouping"
        );
        assertUnsupported(
            baseQuery().having(col("id").eq(1)).build(),
            "grouping"
        );
        assertUnsupported(
            baseQuery().window(WindowDef.of(Identifier.of("w"), OverSpec.def(PartitionBy.of(col("id")), null, null, null))).build(),
            "grouping"
        );
        assertUnsupported(
            baseQuery().orderBy(col("id")).build(),
            "ORDER BY"
        );
        assertUnsupported(
            baseQuery().limit(10).build(),
            "pagination"
        );
        assertUnsupported(
            baseQuery().lockFor(LockMode.UPDATE, java.util.List.of(), false, false).build(),
            "locking"
        );
        assertUnsupported(
            baseQuery().distinct(java.util.List.of(col("id"))).build(),
            "DISTINCT"
        );
        assertUnsupported(
            baseQuery().top(5).build(),
            "TOP"
        );
        assertUnsupported(
            baseQuery().selectModifier(io.sqm.core.SelectModifier.CALC_FOUND_ROWS).build(),
            "modifiers"
        );
        assertUnsupported(
            baseQuery().hint("GATHER_PLAN_STATISTICS").build(),
            "hints"
        );
    }

    @Test
    void rejectsUnsupportedHierarchyAndProjectionShapes() {
        assertUnsupported(
            select(col("id"))
                .from(tbl("categories"))
                .hierarchical(hierarchy(null, prior(col("id")).eq(col("parent_id")), true))
                .build(),
            "NOCYCLE"
        );
        assertUnsupported(
            select(star())
                .from(tbl("categories"))
                .hierarchical(hierarchy(null, prior(col("id")).eq(col("parent_id")), false))
                .build(),
            "expression select items"
        );
        assertUnsupported(
            select(func("upper", col("name")))
                .from(tbl("categories"))
                .hierarchical(hierarchy(null, prior(col("id")).eq(col("parent_id")), false))
                .build(),
            "projected columns and LEVEL"
        );
        assertUnsupported(
            select(col("x", "id"))
                .from(tbl("categories").as("c"))
                .hierarchical(hierarchy(null, prior(col("c", "id")).eq(col("c", "parent_id")), false))
                .build(),
            "source table only"
        );
        assertUnsupported(
            select(col("id"))
                .from(tbl("categories"))
                .hierarchical(hierarchy(null, prior(col("id")).eq(prior(col("parent_id"))), false))
                .build(),
            "exactly one side"
        );
        assertUnsupported(
            select(col("id"))
                .from(tbl("categories"))
                .hierarchical(hierarchy(null, prior(col("id")).gt(col("parent_id")), false))
                .build(),
            "equality predicate"
        );
        assertUnsupported(
            select(col("id"))
                .from(tbl("categories"))
                .hierarchical(hierarchy(null, col("parent_id").isNull(), false))
                .build(),
            "equality predicate"
        );
        assertUnsupported(
            select(col("id"))
                .from(tbl("categories"))
                .hierarchical(hierarchy(null, col("id").eq(col("parent_id")), false))
                .build(),
            "PRIOR column = child column"
        );
        assertUnsupported(
            select(col("id"))
                .from(tbl("categories"))
                .hierarchical(hierarchy(null, prior(col("id")).eq(1), false))
                .build(),
            "PRIOR column = child column"
        );
        assertUnsupported(
            select(col("id"))
                .from(tbl("categories"))
                .hierarchical(hierarchy(null, Expression.literal(1).eq(prior(col("id"))), false))
                .build(),
            "PRIOR column = child column"
        );
        assertUnsupported(
            select(col("id"))
                .from(tbl("categories"))
                .hierarchical(hierarchy(null, prior(Expression.literal(1)).eq(col("parent_id")), false))
                .build(),
            "PRIOR column references"
        );
        assertUnsupported(
            select(col("id"))
                .from(tbl("categories"))
                .hierarchical(hierarchy(prior(col("id")).eq(1), prior(col("id")).eq(col("parent_id")), false))
                .build(),
            "START WITH"
        );
        assertUnsupported(
            select(col("C", "id"))
                .from(tbl("categories").as(Identifier.of("C", QuoteStyle.DOUBLE_QUOTE)))
                .hierarchical(hierarchy(null, prior(col("C", "id")).eq(col("C", "parent_id")), false))
                .build(),
            "source table only"
        );
        assertUnsupported(
            select(ColumnExpr.of(Identifier.of("c", QuoteStyle.DOUBLE_QUOTE), Identifier.of("id")))
                .from(tbl("categories").as(Identifier.of("C", QuoteStyle.DOUBLE_QUOTE)))
                .hierarchical(hierarchy(
                    null,
                    prior(ColumnExpr.of(Identifier.of("c", QuoteStyle.DOUBLE_QUOTE), Identifier.of("id")))
                        .eq(ColumnExpr.of(Identifier.of("c", QuoteStyle.DOUBLE_QUOTE), Identifier.of("parent_id"))),
                    false))
                .build(),
            "source table only"
        );
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

    private static SelectQueryBuilder baseQuery() {
        return select(col("id"))
            .from(tbl("categories"))
            .hierarchical(hierarchy(null, prior(col("id")).eq(col("parent_id")), false));
    }

    private static void assertUnsupported(SelectQuery query, String message) {
        var ex = assertThrows(
            HierarchicalQueryToRecursiveCteTransformer.UnsupportedRewriteException.class,
            () -> new HierarchicalQueryToRecursiveCteTransformer().rewrite(query)
        );
        assertTrue(ex.getMessage().contains(message), ex.getMessage());
    }
}
