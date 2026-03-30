package io.sqm.core.transform;

import io.sqm.core.DeleteStatement;
import io.sqm.core.SelectQuery;
import io.sqm.core.UpdateStatement;
import io.sqm.dsl.Dsl;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.delete;
import static io.sqm.dsl.Dsl.inner;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.set;
import static io.sqm.dsl.Dsl.tbl;
import static io.sqm.dsl.Dsl.unary;
import static io.sqm.dsl.Dsl.update;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class StatementTransformsTest {

    @Test
    void andWhereAddsWhereToSelectWhenMissing() {
        SelectQuery query = select(col("id")).from(tbl("users")).build();

        SelectQuery transformed = StatementTransforms.andWhere(query, col("tenant_id").eq(lit(42)));

        assertNotSame(query, transformed);
        assertEquals(
            "tenant_id",
            transformed.where().matchPredicate()
                .comparison(c -> c.lhs().matchExpression().column(column -> column.name().value()).orElse(null))
                .orElse(null)
        );
    }

    @Test
    void andWhereCombinesExistingWhereWithAndForUpdate() {
        UpdateStatement statement = update(tbl("users"))
            .set(set("name", lit("alice")))
            .where(col("active").eq(lit(true)))
            .build();

        UpdateStatement transformed = StatementTransforms.andWhere(statement, col("tenant_id").eq(lit(42)));

        assertNotSame(statement, transformed);
        assertEquals(
            "active",
            transformed.where().matchPredicate()
                .and(and -> and.lhs().matchPredicate()
                    .comparison(c -> c.lhs().matchExpression().column(column -> column.name().value()).orElse(null))
                    .orElse(null))
                .orElse(null)
        );
        assertEquals(
            "tenant_id",
            transformed.where().matchPredicate()
                .and(and -> and.rhs().matchPredicate()
                    .comparison(c -> c.lhs().matchExpression().column(column -> column.name().value()).orElse(null))
                    .orElse(null))
                .orElse(null)
        );
    }

    @Test
    void andWhereCombinesExistingWhereWithAndForDelete() {
        DeleteStatement statement = delete(tbl("users"))
            .where(col("archived").eq(lit(false)))
            .build();

        DeleteStatement transformed = StatementTransforms.andWhere(statement, col("tenant_id").eq(lit(42)));

        assertNotSame(statement, transformed);
        assertEquals(
            "tenant_id",
            transformed.where().matchPredicate()
                .and(and -> and.rhs().matchPredicate()
                    .comparison(c -> c.lhs().matchExpression().column(column -> column.name().value()).orElse(null))
                    .orElse(null))
                .orElse(null)
        );
    }

    @Test
    void andWherePreservesIdentityForTrueFilter() {
        SelectQuery query = select(col("id"))
            .from(tbl("users"))
            .where(col("active").eq(lit(true)))
            .build();

        SelectQuery transformed = StatementTransforms.andWhere(query, unary(lit(true)));

        assertSame(query, transformed);
    }

    @Test
    void andWhereSupportsGenericStatementDispatch() {
        var statement = Dsl.update(tbl("users"))
            .set(set("name", lit("alice")))
            .build();

        var transformed = StatementTransforms.andWhere(statement, col("tenant_id").eq(lit(42)));

        org.junit.jupiter.api.Assertions.assertInstanceOf(UpdateStatement.class, transformed);
        assertEquals(
            "tenant_id",
            transformed.where().matchPredicate()
                .comparison(c -> c.lhs().matchExpression().column(column -> column.name().value()).orElse(null))
                .orElse(null)
        );
    }

    @Test
    void andWhereIfMissingPreservesIdentityWhenExactConjunctAlreadyExists() {
        SelectQuery query = select(col("id"))
            .from(tbl("users"))
            .where(col("tenant_id").eq(lit(42)))
            .build();

        SelectQuery transformed = StatementTransforms.andWhereIfMissing(query, col("tenant_id").eq(lit(42)));

        assertSame(query, transformed);
    }

    @Test
    void andWhereIfMissingAppendsOnlyNewConjunctsFromCompositeFilter() {
        SelectQuery query = select(col("id"))
            .from(tbl("users"))
            .where(col("tenant_id").eq(lit(42)))
            .build();

        SelectQuery transformed = StatementTransforms.andWhereIfMissing(
            query,
            col("tenant_id").eq(lit(42)).and(col("active").eq(lit(true)))
        );

        assertNotSame(query, transformed);
        assertEquals(
            "tenant_id",
            transformed.where().matchPredicate()
                .and(and -> and.lhs().matchPredicate()
                    .comparison(c -> c.lhs().matchExpression().column(column -> column.name().value()).orElse(null))
                    .orElse(null))
                .orElse(null)
        );
        assertEquals(
            "active",
            transformed.where().matchPredicate()
                .and(and -> and.rhs().matchPredicate()
                    .comparison(c -> c.lhs().matchExpression().column(column -> column.name().value()).orElse(null))
                    .orElse(null))
                .orElse(null)
        );
    }

    @Test
    void andWhereIfMissingSupportsGenericStatementDispatch() {
        var statement = Dsl.delete(tbl("users"))
            .where(col("tenant_id").eq(lit(42)))
            .build();

        var transformed = StatementTransforms.andWhereIfMissing(
            statement,
            col("tenant_id").eq(lit(42)).and(col("active").eq(lit(true)))
        );

        org.junit.jupiter.api.Assertions.assertInstanceOf(DeleteStatement.class, transformed);
        assertEquals(
            "active",
            transformed.where().matchPredicate()
                .and(and -> and.rhs().matchPredicate()
                    .comparison(c -> c.lhs().matchExpression().column(column -> column.name().value()).orElse(null))
                    .orElse(null))
                .orElse(null)
        );
    }

    @Test
    void andWherePerTableRecursivelyInjectsAliasTargetedFiltersIntoSelect() {
        SelectQuery query = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .join(inner(tbl("orders").as("o")).on(col("u", "id").eq(col("o", "user_id"))))
            .build();

        SelectQuery transformed = (SelectQuery) StatementTransforms.andWherePerTableRecursively(query, binding -> switch (binding.qualifier().value()) {
            case "u" -> col("u", "tenant_id").eq(lit(42));
            case "o" -> col("o", "tenant_id").eq(lit(42));
            default -> null;
        });

        assertNotSame(query, transformed);
        assertEquals(
            "u",
            transformed.where().matchPredicate()
                .and(and -> and.lhs().matchPredicate()
                    .comparison(c -> c.lhs().matchExpression().column(column -> column.tableAlias().value()).orElse(null))
                    .orElse(null))
                .orElse(null)
        );
        assertEquals(
            "o",
            transformed.where().matchPredicate()
                .and(and -> and.rhs().matchPredicate()
                    .comparison(c -> c.lhs().matchExpression().column(column -> column.tableAlias().value()).orElse(null))
                    .orElse(null))
                .orElse(null)
        );
    }

    @Test
    void andWherePerTableRecursivelyCombinesWithExistingWhereForUpdate() {
        UpdateStatement statement = update(tbl("users").as("u"))
            .set(set("name", lit("alice")))
            .where(col("u", "active").eq(lit(true)))
            .build();

        UpdateStatement transformed = (UpdateStatement) StatementTransforms.andWherePerTableRecursively(statement, binding ->
            "u".equals(binding.qualifier().value()) ? col("u", "tenant_id").eq(lit(42)) : null
        );

        assertNotSame(statement, transformed);
        assertEquals(
            "active",
            transformed.where().matchPredicate()
                .and(and -> and.lhs().matchPredicate()
                    .comparison(c -> c.lhs().matchExpression().column(column -> column.name().value()).orElse(null))
                    .orElse(null))
                .orElse(null)
        );
        assertEquals(
            "tenant_id",
            transformed.where().matchPredicate()
                .and(and -> and.rhs().matchPredicate()
                    .comparison(c -> c.lhs().matchExpression().column(column -> column.name().value()).orElse(null))
                    .orElse(null))
                .orElse(null)
        );
    }

    @Test
    void andWherePerTableRecursivelyTraversesNestedQueries() {
        SelectQuery inner = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .build();
        SelectQuery outer = select(col("sq", "id"))
            .from(tbl(inner).as("sq"))
            .build();

        SelectQuery transformed = (SelectQuery) StatementTransforms.andWherePerTableRecursively(outer, binding ->
            "u".equals(binding.qualifier().value()) ? col("u", "tenant_id").eq(lit(42)) : null
        );

        SelectQuery innerTransformed = transformed.from().<SelectQuery>matchTableRef()
            .query(queryTable -> (SelectQuery) queryTable.query())
            .orElseThrow(AssertionError::new);
        assertEquals(
            "tenant_id",
            innerTransformed.where().matchPredicate()
                .comparison(c -> c.lhs().matchExpression().column(column -> column.name().value()).orElse(null))
                .orElse(null)
        );
        assertSame(null, transformed.where());
    }

    @Test
    void andWherePerTableOnlyAffectsCurrentQueryBlock() {
        SelectQuery inner = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .build();
        SelectQuery outer = select(col("sq", "id"))
            .from(tbl(inner).as("sq"))
            .build();

        SelectQuery transformed = StatementTransforms.andWherePerTable(outer, binding ->
            "u".equals(binding.qualifier().value()) ? col("u", "tenant_id").eq(lit(42)) : null
        );

        SelectQuery innerTransformed = transformed.from().<SelectQuery>matchTableRef()
            .query(queryTable -> (SelectQuery) queryTable.query())
            .orElseThrow(AssertionError::new);
        assertSame(inner, innerTransformed);
        assertSame(null, transformed.where());
    }

    @Test
    void andWherePerTableRecursivelyPreservesIdentityWhenResolverAddsNothing() {
        SelectQuery query = select(col("id")).from(tbl("users")).build();

        SelectQuery transformed = (SelectQuery) StatementTransforms.andWherePerTableRecursively(query, binding -> null);

        assertSame(query, transformed);
    }
}
