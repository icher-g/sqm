package io.sqm.core.transform;

import io.sqm.core.Expression;
import io.sqm.core.Identifier;
import io.sqm.core.Query;
import io.sqm.core.TableRef;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static io.sqm.dsl.Dsl.col;

class ColumnQualificationTransformerTest {

    @Test
    void qualifies_unqualified_columns_when_resolver_returns_qualifier() {
        var query = Query.select(col("id")).from(TableRef.table(Identifier.of("users"))).build();
        var transformer = ColumnQualificationTransformer.of((column, visible) -> {
            assertEquals("id", column);
            assertEquals(1, visible.size());
            assertEquals("users", visible.getFirst().qualifier().value());
            return ColumnQualification.qualified(Identifier.of("u"));
        });

        var transformed = transformer.apply(query);

        assertNotSame(query, transformed);
        var select = (io.sqm.core.SelectQuery) transformed;
        var item = (io.sqm.core.ExprSelectItem) select.items().getFirst();
        var column = (io.sqm.core.ColumnExpr) item.expr();
        assertEquals("u", column.tableAlias().value());
        assertEquals("id", column.name().value());
    }

    @Test
    void preserves_already_qualified_columns_and_unresolved_columns() {
        var qualified = Query.select(col("u", "id")).from(TableRef.table(Identifier.of("users"))).build();
        var unresolved = Query.select(col("id")).from(TableRef.table(Identifier.of("users"))).build();
        var noop = ColumnQualificationTransformer.of((column, visible) -> ColumnQualification.unresolved());

        assertSame(qualified, noop.apply(qualified));
        assertSame(unresolved, noop.apply(unresolved));
    }

    @Test
    void throws_for_ambiguous_resolution() {
        var query = Query.select(col("id")).from(TableRef.table(Identifier.of("users"))).build();
        var transformer = ColumnQualificationTransformer.of((column, visible) -> ColumnQualification.ambiguous());

        assertThrows(
            ColumnQualificationTransformer.AmbiguousColumnQualificationException.class,
            () -> transformer.apply(query)
        );
    }

    @Test
    void includes_outer_scope_bindings_for_nested_subqueries() {
        var subquery = Query.select(col("id")).from(TableRef.table(Identifier.of("orders"))).build();
        var query = Query.select(Expression.subquery(subquery)).from(TableRef.table(Identifier.of("users"))).build();
        var transformer = ColumnQualificationTransformer.of((column, visible) -> {
            if ("id".equals(column)) {
                List<String> qualifiers = visible.stream().map(v -> v.qualifier().value()).toList();
                if (qualifiers.contains("orders")) {
                    return ColumnQualification.qualified(Identifier.of("orders"));
                }
            }
            return ColumnQualification.unresolved();
        });

        var transformed = transformer.apply(query);

        assertNotSame(query, transformed);
    }
}


