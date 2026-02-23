package io.sqm.core.transform;

import io.sqm.core.Expression;
import io.sqm.core.Query;
import io.sqm.core.TableRef;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ColumnQualificationTransformerTest {

    @Test
    void qualifies_unqualified_columns_when_resolver_returns_qualifier() {
        var query = Query.select(Expression.column("id")).from(TableRef.table("users")).build();
        var transformer = ColumnQualificationTransformer.of((column, visible) -> {
            assertEquals("id", column);
            assertEquals(1, visible.size());
            assertEquals("users", visible.getFirst().qualifier());
            return ColumnQualification.qualified("u");
        });

        var transformed = transformer.apply(query);

        assertNotSame(query, transformed);
        var select = (io.sqm.core.SelectQuery) transformed;
        var item = (io.sqm.core.ExprSelectItem) select.items().getFirst();
        var column = (io.sqm.core.ColumnExpr) item.expr();
        assertEquals("u", column.tableAlias());
        assertEquals("id", column.name());
    }

    @Test
    void preserves_already_qualified_columns_and_unresolved_columns() {
        var qualified = Query.select(Expression.column("u", "id")).from(TableRef.table("users")).build();
        var unresolved = Query.select(Expression.column("id")).from(TableRef.table("users")).build();
        var noop = ColumnQualificationTransformer.of((column, visible) -> ColumnQualification.unresolved());

        assertSame(qualified, noop.apply(qualified));
        assertSame(unresolved, noop.apply(unresolved));
    }

    @Test
    void throws_for_ambiguous_resolution() {
        var query = Query.select(Expression.column("id")).from(TableRef.table("users")).build();
        var transformer = ColumnQualificationTransformer.of((column, visible) -> ColumnQualification.ambiguous());

        assertThrows(
            ColumnQualificationTransformer.AmbiguousColumnQualificationException.class,
            () -> transformer.apply(query)
        );
    }

    @Test
    void includes_outer_scope_bindings_for_nested_subqueries() {
        var subquery = Query.select(Expression.column("id")).from(TableRef.table("orders")).build();
        var query = Query.select(Expression.subquery(subquery)).from(TableRef.table("users")).build();
        var transformer = ColumnQualificationTransformer.of((column, visible) -> {
            if ("id".equals(column)) {
                List<String> qualifiers = visible.stream().map(VisibleTableBinding::qualifier).toList();
                if (qualifiers.contains("orders")) {
                    return ColumnQualification.qualified("orders");
                }
            }
            return ColumnQualification.unresolved();
        });

        var transformed = transformer.apply(query);

        assertNotSame(query, transformed);
    }
}
