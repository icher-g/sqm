package io.sqm.it;

import io.sqm.core.Query;
import io.sqm.core.Statement;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.mysql.spi.MySqlSpecs;
import io.sqm.parser.oracle.spi.OracleSpecs;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.mysql.spi.MySqlDialect;
import io.sqm.render.oracle.spi.OracleDialect;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.sqlserver.spi.SqlServerDialect;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelBoundaryIntegrationTest {
    private static final String SQL = "SELECT sales FROM sales_data MODEL DIMENSION BY (year_key) MEASURES (sales) (sales[2026] = sales[2025] + 1)";

    @Test
    void nonOracleDialectsRejectModelAtBothBoundaries() {
        for (var specs : List.of(new AnsiSpecs(), new PostgresSpecs(), new MySqlSpecs(), new SqlServerSpecs())) {
            var parsed = ParseContext.of(specs).parse(Query.class, SQL);
            assertTrue(parsed.isError(), specs.getClass().getSimpleName());
            assertTrue(Objects.requireNonNull(parsed.errorMessage()).contains("MODEL"), parsed::errorMessage);
        }
        var query = select(col("sales")).model(model().dimension("year_key").measure("sales")
            .rule(cellTarget("sales", 2026), lit(1)).build()).build();
        for (var dialect : List.of(new AnsiDialect(), new PostgresDialect(), new MySqlDialect(), new SqlServerDialect())) {
            var ctx = RenderContext.of(dialect);
            var writer = new DefaultSqlWriter(ctx);
            assertThrows(UnsupportedDialectFeatureException.class, () -> ctx.render(query, writer));
            assertEquals("", writer.toText(List.of()).sql());
            assertThrows(UnsupportedDialectFeatureException.class, () -> ctx.render(query.model()));
        }
    }

    @Test
    void oracleQuerySourcesCarryModelThroughExistingDmlAndSubqueryPaths() {
        var parser = ParseContext.of(new OracleSpecs());
        var renderer = RenderContext.of(new OracleDialect());
        for (var sql : List.of(SQL, "INSERT INTO forecasts (sales) " + SQL,
            "SELECT sales FROM (" + SQL + ") calculated",
            "WITH forecast AS (" + SQL + ") SELECT sales FROM forecast")) {
            var parsed = parser.parse(Statement.class, sql);
            assertTrue(parsed.ok(), parsed::errorMessage);
            var rendered = renderer.render(parsed.value()).sql();
            var reparsed = parser.parse(Statement.class, rendered);
            assertTrue(reparsed.ok(), reparsed::errorMessage);
            assertEquals(parsed.value(), reparsed.value(), rendered);
        }
    }
}
