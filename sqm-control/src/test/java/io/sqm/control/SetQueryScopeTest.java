package io.sqm.control;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.control.execution.ExecutionContext;
import io.sqm.control.execution.ExecutionMode;
import io.sqm.control.pipeline.SqlStatementValidator;
import io.sqm.core.CompositeQuery;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.mysql.spi.MySqlSpecs;
import io.sqm.parser.oracle.spi.OracleSpecs;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.Specs;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.mysql.spi.MySqlDialect;
import io.sqm.render.oracle.spi.OracleDialect;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.sqlserver.spi.SqlServerDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.select;
import static org.junit.jupiter.api.Assertions.*;

class SetQueryScopeTest {
    @Test
    void validationChecksPaginationOnTheCompositeOwner() {
        var ctx = ParseContext.of(new PostgresSpecs());
        var validator = SqlStatementValidator.standard(CatalogSchema.of());
        var execution = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);
        var valid = parse(ctx, "SELECT 1 AS id UNION SELECT 2 AS id ORDER BY id LIMIT 5 OFFSET 2");
        var validated = validator.validate(valid, execution);
        assertFalse(validated.isFailed(), validated::message);
        var invalid = parse(ctx, "SELECT 1 AS id UNION SELECT 2 AS id ORDER BY id LIMIT 'invalid'");
        assertTrue(validator.validate(invalid, execution).isFailed());
    }

    @Test
    void compositeRenderingUsesDialectPaginationRestrictions() {
        var offsetOnly = select(lit(1L)).build().union(select(lit(2L)).build()).offset(2L);
        assertThrows(UnsupportedOperationException.class,
            () -> RenderContext.of(new MySqlDialect()).render(offsetOnly));
        var limitAll = parse(ParseContext.of(new PostgresSpecs()), "SELECT 1 UNION SELECT 2 LIMIT ALL");
        assertThrows(UnsupportedOperationException.class,
            () -> RenderContext.of(new SqlServerDialect()).render(limitAll));
    }

    @Test
    void globalPaginationRoundTripsAcrossDialects() {
        check(new AnsiSpecs(), RenderContext.of(new AnsiDialect()), "OFFSET 2 ROWS FETCH NEXT 5 ROWS ONLY");
        check(new PostgresSpecs(), RenderContext.of(new PostgresDialect()), "LIMIT 5 OFFSET 2");
        check(new MySqlSpecs(), RenderContext.of(new MySqlDialect()), "LIMIT 2, 5");
        check(new SqlServerSpecs(), RenderContext.of(new SqlServerDialect()), "OFFSET 2 ROWS FETCH NEXT 5 ROWS ONLY");
        check(new OracleSpecs(), RenderContext.of(new OracleDialect()), "OFFSET 2 ROWS FETCH NEXT 5 ROWS ONLY");
    }

    @Test
    void postgresLocalAndGlobalTailsSurviveRenderingTogether() {
        var ctx = ParseContext.of(new PostgresSpecs());
        var query = parse(ctx, """
            SELECT id FROM users UNION
            (SELECT id FROM orders ORDER BY id DESC LIMIT 4 OFFSET 1)
            ORDER BY id LIMIT 5 OFFSET 2
            """);
        assertEquals(query, parse(ctx, RenderContext.of(new PostgresDialect()).render(query).sql()));
        assertEquals(lit(4L), ((SelectQuery) query.terms().getLast()).limitOffset().limit());
        assertEquals(lit(5L), query.limitOffset().limit());
    }

    private void check(Specs specs, RenderContext render, String pagination) {
        var ctx = ParseContext.of(specs);
        for (var terms : new String[]{"SELECT id FROM users UNION SELECT id FROM orders",
            "(SELECT id FROM users) UNION (SELECT id FROM orders)"}) {
            var query = parse(ctx, terms + " ORDER BY id DESC " + pagination);
            assertNotNull(query.orderBy());
            assertEquals(lit(5L), query.limitOffset().limit());
            assertEquals(lit(2L), query.limitOffset().offset());
            for (var term : query.terms()) {
                var select = assertInstanceOf(SelectQuery.class, term);
                assertNull(select.orderBy());
                assertTrue(select.limitOffset() == null || select.limitOffset().limit() == null);
            }
            assertEquals(query, parse(ctx, render.render(query).sql()));
        }
    }

    private CompositeQuery parse(ParseContext ctx, String sql) {
        var result = ctx.parse(Query.class, sql);
        assertTrue(result.ok(), result::errorMessage);
        return assertInstanceOf(CompositeQuery.class, result.value());
    }
}
