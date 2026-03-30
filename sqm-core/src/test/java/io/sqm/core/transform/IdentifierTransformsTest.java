package io.sqm.core.transform;

import io.sqm.core.UpdateStatement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.inner;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.set;
import static io.sqm.dsl.Dsl.tbl;
import static io.sqm.dsl.Dsl.update;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class IdentifierTransformsTest {

    @Test
    void renameColumnRewritesMatchingColumnReferencesOnly() {
        var statement = update(tbl("users"))
            .set(set("name", lit("alice")))
            .where(col("u", "id").eq(col("u", "manager_id")))
            .result(col("u", "id"))
            .build();

        UpdateStatement transformed = IdentifierTransforms.renameColumn(statement, "u", "id", "user_id");

        assertNotSame(statement, transformed);
        assertEquals(
            "user_id",
            transformed.where().matchPredicate()
                .comparison(c -> c.lhs().matchExpression().column(column -> column.name().value()).orElse(null))
                .orElse(null)
        );
        assertEquals(
            "manager_id",
            transformed.where().matchPredicate()
                .comparison(c -> c.rhs().matchExpression().column(column -> column.name().value()).orElse(null))
                .orElse(null)
        );
        assertEquals(
            "user_id",
            transformed.result().items().getFirst().matchResultItem()
                .expr(item -> item.expr().matchExpression().column(column -> column.name().value()).orElse(null))
                .orElse(null)
        );
    }

    @Test
    void rewriteColumnsSupportsTargetedCustomMappings() {
        var statement = update(tbl("users"))
            .set(set("name", lit("alice")))
            .where(col("id").eq(lit(1)))
            .result(col("id"))
            .build();

        UpdateStatement transformed = IdentifierTransforms.rewriteColumns(statement, column ->
            "id".equals(column.name().value()) ? col(id("user_id")) : column
        );

        assertNotSame(statement, transformed);
        assertEquals(
            "user_id",
            transformed.where().matchPredicate()
                .comparison(c -> c.lhs().matchExpression().column(column -> column.name().value()).orElse(null))
                .orElse(null)
        );
    }

    @Test
    void renameColumnPreservesIdentityWhenNothingMatches() {
        var statement = update(tbl("users"))
            .set(set("name", lit("alice")))
            .where(col("id").eq(lit(1)))
            .build();

        var transformed = IdentifierTransforms.renameColumn(statement, "u", "id", "user_id");

        assertSame(statement, transformed);
    }

    @Test
    void remapColumnsSupportsQualifierAwareRuntimeMappings() {
        var query = select(col("u", "tenant_id"), col("o", "tenant_id"))
            .from(tbl("users").as("u"))
            .join(inner(tbl("orders").as("o")).on(col("u", "id").eq(col("o", "user_id"))))
            .where(col("u", "tenant_id").eq(col("o", "tenant_id")))
            .build();

        var transformed = IdentifierTransforms.remapColumns(query, (qualifier, column) -> {
            if (!"tenant_id".equals(column.value())) {
                return column;
            }
            if (qualifier != null && "u".equals(qualifier.value())) {
                return id("customer_id");
            }
            if (qualifier != null && "o".equals(qualifier.value())) {
                return id("account_tenant_id");
            }
            return column;
        });

        Assertions.assertInstanceOf(io.sqm.core.SelectQuery.class, transformed);
        assertNotSame(query, transformed);
        assertEquals(
            "customer_id",
            transformed.items().getFirst().matchSelectItem()
                .expr(item -> item.expr().matchExpression().column(column -> column.name().value()).orElse(null))
                .orElse(null)
        );
        assertEquals(
            "account_tenant_id",
            transformed.items().get(1).matchSelectItem()
                .expr(item -> item.expr().matchExpression().column(column -> column.name().value()).orElse(null))
                .orElse(null)
        );
        assertEquals(
            "customer_id",
            transformed.where().matchPredicate()
                .comparison(c -> c.lhs().matchExpression().column(column -> column.name().value()).orElse(null))
                .orElse(null)
        );
        assertEquals(
            "account_tenant_id",
            transformed.where().matchPredicate()
                .comparison(c -> c.rhs().matchExpression().column(column -> column.name().value()).orElse(null))
                .orElse(null)
        );
    }
}
