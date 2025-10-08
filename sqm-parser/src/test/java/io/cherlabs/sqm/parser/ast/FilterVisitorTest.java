package io.cherlabs.sqm.parser.ast;

import io.cherlabs.sqm.core.*;
import io.cherlabs.sqm.core.views.Columns;
import io.cherlabs.sqm.parser.expr.ExprParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration-style tests for FilterVisitor:
 * ExprParser (text -> AST)  +  FilterVisitor (AST -> Filter model)
 * <p>
 * If your concrete class names differ slightly, tweak the assertions where noted.
 */
class FilterVisitorTest {

    /* ------------------------- helpers ------------------------- */

    private static <T> T as(Class<T> type, Object v) {
        assertNotNull(v, "value is null");
        assertTrue(type.isInstance(v), () -> "Expected " + type.getSimpleName() + " but was " + v.getClass().getSimpleName());
        return type.cast(v);
    }

    private Filter visit(String text) {
        Expr ast = new ExprParser(text).parseExpr();
        return ast.accept(new FilterVisitor());
    }

    /* --------------------- single-column ops ------------------- */

    @Test
    @DisplayName("= maps to ColumnFilter(Eq) with Values.Single")
    void eq_single() {
        Filter f = visit("qty = 5");
        ColumnFilter cf = as(ColumnFilter.class, f);
        assertEquals(ColumnFilter.Operator.Eq, cf.op());
        Values.Single v = as(Values.Single.class, cf.values());
        assertEquals(5L, v.value()); // adjust if your numbers are Integer/BigDecimal
        assertEquals("qty", cf.columnAs(NamedColumn.class).name());
    }

    @Test
    @DisplayName("!= and <> map to ColumnFilter(Ne)")
    void ne_single() {
        ColumnFilter cf1 = as(ColumnFilter.class, visit("qty != 5"));
        assertEquals(ColumnFilter.Operator.Ne, cf1.op());

        ColumnFilter cf2 = as(ColumnFilter.class, visit("qty <> 5"));
        assertEquals(ColumnFilter.Operator.Ne, cf2.op());
    }

    @Test
    @DisplayName(">, >=, <, <= map to respective ColumnFilter.Operator")
    void comparisons() {
        assertEquals(ColumnFilter.Operator.Gt, as(ColumnFilter.class, visit("price > 10")).op());
        assertEquals(ColumnFilter.Operator.Gte, as(ColumnFilter.class, visit("price >= 10")).op());
        assertEquals(ColumnFilter.Operator.Lt, as(ColumnFilter.class, visit("price < 10")).op());
        assertEquals(ColumnFilter.Operator.Lte, as(ColumnFilter.class, visit("price <= 10")).op());
    }

    @Test
    @DisplayName("LIKE produces ColumnFilter(Like) + Values.Single")
    void like_op() {
        ColumnFilter cf = as(ColumnFilter.class, visit("name LIKE '%abc%'"));
        assertEquals(ColumnFilter.Operator.Like, cf.op());
        assertEquals("%abc%", as(Values.Single.class, cf.values()).value());
        assertEquals("name", cf.columnAs(NamedColumn.class).name());
    }

    @Test
    @DisplayName("BETWEEN produces ColumnFilter(Ranges) + Values.Range")
    void between_op() {
        ColumnFilter cf = as(ColumnFilter.class, visit("price BETWEEN 10 AND 20"));
        assertEquals(ColumnFilter.Operator.Range, cf.op());
        Values.Range r = as(Values.Range.class, cf.values());
        assertEquals(10L, r.min());
        assertEquals(20L, r.max());
        assertEquals("price", cf.columnAs(NamedColumn.class).name());
    }

    @Test
    @DisplayName("IN (list) -> ColumnFilter(In) + Values.ListVals")
    void in_list() {
        ColumnFilter cf = as(ColumnFilter.class, visit("category IN (1, 2, 3)"));
        assertEquals(ColumnFilter.Operator.In, cf.op());
        Values.ListValues v = as(Values.ListValues.class, cf.values());
        assertEquals(List.of(1L, 2L, 3L), v.items());
        assertEquals("category", cf.columnAs(NamedColumn.class).name());
    }

    @Test
    @DisplayName("NOT IN (list) -> ColumnFilter(NotIn)")
    void not_in_list() {
        ColumnFilter cf = as(ColumnFilter.class, visit("status NOT IN ('A','B')"));
        assertEquals(ColumnFilter.Operator.NotIn, cf.op());
        assertEquals(List.of("A", "B"), as(Values.ListValues.class, cf.values()).items());
        assertEquals("status", cf.columnAs(NamedColumn.class).name());
    }

    /* ------------------------ tuple IN ------------------------- */

    @Test
    @DisplayName("Tuple IN: (a,b) IN ((1,2),(3,4)) -> TupleColumnFilter + Values.Tuples")
    void tuple_in() {
        TupleFilter tf = as(TupleFilter.class, visit("(a,b) IN ((1,2),(3,4))"));
        assertEquals(TupleFilter.Operator.In, tf.operator());
        assertEquals(2, tf.columns().size());
        assertEquals(Optional.of("a"), Columns.name(tf.columns().get(0)));
        assertEquals(Optional.of("b"), Columns.name(tf.columns().get(1)));

        Values.Tuples t = as(Values.Tuples.class, tf.values());
        assertEquals(List.of(List.of(1L, 2L), List.of(3L, 4L)), t.rows());
    }

    /* --------------- booleans: NOT > AND > OR ------------------ */

    @Test
    @DisplayName("AND groups before OR; NOT has highest precedence")
    void boolean_precedence() {
        // status IN ('A','B') AND (price BETWEEN 10 AND 20 OR NOT name LIKE '%test%')
        Filter root = visit("status IN ('A','B') AND (price BETWEEN 10 AND 20 OR NOT name LIKE '%test%')");
        CompositeFilter and = as(CompositeFilter.class, root);
        assertEquals(CompositeFilter.Operator.And, and.op());
        assertEquals(2, and.filters().size());

        assertInstanceOf(ColumnFilter.class, and.filters().get(0)); // left IN

        CompositeFilter or = as(CompositeFilter.class, and.filters().get(1));
        assertEquals(CompositeFilter.Operator.Or, or.op());
        assertEquals(2, or.filters().size());

        CompositeFilter not = as(CompositeFilter.class, or.filters().get(1));
        assertEquals(CompositeFilter.Operator.Not, not.op());
        assertEquals(1, not.filters().size());
        assertInstanceOf(ColumnFilter.class, not.filters().get(0)); // LIKE inside NOT
    }

    /* -------------------------- literals ----------------------- */

    @Test
    @DisplayName("Escaped single quote: 'O''Reilly'")
    void escaped_quote() {
        ColumnFilter cf = as(ColumnFilter.class, visit("name LIKE 'O''Reilly'"));
        assertEquals("O'Reilly", as(Values.Single.class, cf.values()).value());
    }

    @Test
    @DisplayName("TRUE / FALSE / NULL literals")
    void boolean_and_null_literals() {
        assertEquals(Boolean.TRUE, as(Values.Single.class, as(ColumnFilter.class, visit("active = TRUE")).values()).value());
        assertEquals(Boolean.FALSE, as(Values.Single.class, as(ColumnFilter.class, visit("deleted = FALSE")).values()).value());
        assertNull(as(Values.Single.class, as(ColumnFilter.class, visit("note = NULL")).values()).value());
    }

    /* ----------------------- mixed shapes ---------------------- */

    @Test
    @DisplayName("Composite with tuple IN and comparison")
    void mix_tuple_and_cmp() {
        Filter f = visit("(a,b) IN ((1,2)) AND qty >= 5");
        CompositeFilter and = as(CompositeFilter.class, f);
        assertEquals(2, and.filters().size());
        assertTrue(and.filters().stream().anyMatch(x -> x instanceof TupleFilter));
        assertTrue(and.filters().stream().anyMatch(x -> x instanceof ColumnFilter));
    }
}
