package io.sqm.parser.postgresql;

import io.sqm.core.AnyAllPredicate;
import io.sqm.core.ColumnExpr;
import io.sqm.core.Expression;
import io.sqm.core.Predicate;
import io.sqm.core.Quantifier;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnyAllPredicateParserTest {
    private final ParseContext ctx = ParseContext.of(new PostgresSpecs());

    @Test
    void parsesAnyWithArrayExpressionSource() {
        var result = ctx.parse(Predicate.class, "category_id = ANY (path)");

        assertTrue(result.ok(), result::errorMessage);
        var predicate = assertInstanceOf(AnyAllPredicate.class, result.value());
        assertEquals(Quantifier.ANY, predicate.quantifier());
        assertInstanceOf(ColumnExpr.class, predicate.source());
    }

    @Test
    void parsesAnyWithQualifiedArrayExpressionSourceInQuery() {
        var result = ctx.parse(Query.class, """
            SELECT c.category_id
            FROM categories c
            WHERE NOT c.category_id = ANY(ct.path)
            """);

        assertTrue(result.ok(), result::errorMessage);
        var query = assertInstanceOf(SelectQuery.class, result.value());
        var not = assertInstanceOf(io.sqm.core.NotPredicate.class, query.where());
        var predicate = assertInstanceOf(AnyAllPredicate.class, not.inner());
        var source = assertInstanceOf(Expression.class, predicate.source());
        assertEquals("path", source.matchExpression().column(c -> c.name().value()).orElse(null));
    }

    @Test
    void preservesStandardAnyWithSubquerySource() {
        var result = ctx.parse(Predicate.class, "category_id = ANY (SELECT category_id FROM categories)");

        assertTrue(result.ok(), result::errorMessage);
        var predicate = assertInstanceOf(AnyAllPredicate.class, result.value());
        assertInstanceOf(Query.class, predicate.source());
    }
}
