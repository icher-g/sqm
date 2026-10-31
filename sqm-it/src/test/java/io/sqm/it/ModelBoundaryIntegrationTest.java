package io.sqm.it;

import com.fasterxml.jackson.databind.DeserializationFeature;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.core.ColumnExpr;
import io.sqm.core.Node;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.Statement;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.SqlTranspiler;
import io.sqm.transpile.TranspileStatus;
import io.sqm.core.transform.RecursiveNodeTransformer;
import io.sqm.core.walk.RecursiveNodeVisitor;
import io.sqm.json.SqmJsonMixins;
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
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.oracle.OracleValidationDialect;
import io.sqm.validate.schema.SchemaStatementValidator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelBoundaryIntegrationTest {
    private static final String SQL = "SELECT sales FROM sales_data MODEL DIMENSION BY (year_key) MEASURES (sales) (sales[2026] = sales[2025] + 1)";

    @Test
    void nativeTranspilationPreservesModelAcrossCteSubqueryAndEveryExistingDmlSource() {
        var parser = ParseContext.of(new OracleSpecs());
        var nativeTarget = SqlTranspiler.builder().sourceDialect(SqlDialectId.ORACLE).targetDialect(SqlDialectId.ORACLE)
            .targetSchema(CatalogSchema.allowEverything()).build();
        for (var sql : List.of(SQL, "WITH forecast AS (" + SQL + ") SELECT sales FROM forecast",
            "INSERT INTO forecasts (sales) " + SQL,
            "UPDATE forecasts SET sales = (" + SQL + ")",
            "DELETE FROM forecasts WHERE EXISTS (" + SQL + ")",
            "MERGE INTO forecasts f USING (" + SQL + ") s ON (f.sales = s.sales) WHEN MATCHED THEN UPDATE SET f.sales = s.sales")) {
            var original = parser.parse(Statement.class, sql);
            assertTrue(original.ok(), original::errorMessage);
            var exact = nativeTarget.transpile(original.value());
            assertEquals(TranspileStatus.SUCCESS, exact.status(), () -> exact.problems().toString());
            assertSame(original.value(), exact.transpiledAst().orElseThrow());
            assertEquals(original.value(), parser.parse(Statement.class, exact.sql().orElseThrow()).value());
            for (var target : List.of(SqlDialectId.ANSI, SqlDialectId.MYSQL, SqlDialectId.POSTGRESQL, SqlDialectId.SQLSERVER)) {
                var result = SqlTranspiler.builder().sourceDialect(SqlDialectId.ORACLE).targetDialect(target).build().transpile(original.value());
                assertEquals(TranspileStatus.UNSUPPORTED, result.status());
                assertSame(original.value(), result.transpiledAst().orElseThrow());
                assertTrue(result.sql().isEmpty());
                assertTrue(result.problems().stream().allMatch(p -> p.code().equals("UNSUPPORTED_MODEL_CLAUSE")
                    && p.sourceDialect().equals(SqlDialectId.ORACLE) && p.targetDialect().equals(target) && p.clausePath().contains("model")));
            }
        }
    }

    @Test
    void semanticIssuesAreReportedByValidationNotParsingOrRendering() {
        var parser = ParseContext.of(new OracleSpecs());
        var renderer = RenderContext.of(new OracleDialect());
        var validator = SchemaStatementValidator.of(
            CatalogSchema.allowEverything(), OracleValidationDialect.of());
        for (var sql : List.of(
            "SELECT sales FROM sales_data MODEL DIMENSION BY (year_key) MEASURES (sales) "
                + "RULES ITERATE (-1) (sales[FOR year_key FROM 1 TO 10001 INCREMENT 1] = 1)",
            "SELECT sales FROM sales_data MODEL DIMENSION BY (product, year_key) MEASURES (sales) "
                + "RULES (sales[FOR (product, year_key) IN (SELECT product FROM years)] = 1)")) {
            var parsed = parser.parse(Query.class, sql);
            assertTrue(parsed.ok(), parsed::errorMessage);
            var rendered = renderer.render(parsed.value()).sql();
            assertEquals(parsed.value(), parser.parse(Query.class, rendered).value());
            var result = validator.validate(parsed.value());
            assertTrue(result.problems().stream().anyMatch(problem ->
                problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                    && problem.clausePath().startsWith("model.main.rules")), result.problems().toString());
        }
    }

    @Test
    void advancedReferenceAndForQueriesRemainVisibleToJsonVisitorsAndTransformers() throws Exception {
        var parser = ParseContext.of(new OracleSpecs());
        var renderer = RenderContext.of(new OracleDialect());
        var sql = "SELECT sales FROM sales_data MODEL "
            + "REFERENCE rates ON (SELECT code, rate FROM fx) DIMENSION BY (code) MEASURES (rate) "
            + "DIMENSION BY (year_key) MEASURES (sales) RULES ITERATE (2) "
            + "UNTIL (PREVIOUS(sales[2026]) = sales[2026]) "
            + "(sales[FOR year_key IN (SELECT forecast_year FROM years)] = rates.rate['USD'] * ITERATION_NUMBER)";
        var parsed = parser.parse(Query.class, sql);
        assertTrue(parsed.ok(), parsed::errorMessage);
        var original = parsed.value();
        // Preserve the SQL parser's Long representation for integer literals.
        var mapper = SqmJsonMixins.createDefault().enable(DeserializationFeature.USE_LONG_FOR_INTS);
        assertEquals(original, mapper.readValue(mapper.writeValueAsString(original), Query.class));
        var visited = new ArrayList<String>();
        original.accept(new RecursiveNodeVisitor<Void>() {
            @Override protected Void defaultResult() { return null; }
            @Override public Void visitColumnExpr(ColumnExpr node) {
                visited.add(node.name().value());
                return null;
            }
        });
        assertTrue(visited.containsAll(List.of("code", "rate", "forecast_year")));
        assertSame(original, new RecursiveNodeTransformer() {}.transform(original));
        var transformed = (SelectQuery) new RecursiveNodeTransformer() {
            @Override public Node visitColumnExpr(ColumnExpr node) {
                return node.name().value().equals("forecast_year") ? col("next_year") : node;
            }
        }.transform(original);
        assertNotSame(original, transformed);
        assertSame(((SelectQuery) original).model().references().getFirst(), transformed.model().references().getFirst());
        var rendered = renderer.render(transformed).sql();
        assertTrue(rendered.contains("SELECT next_year"));
        var reparsed = parser.parse(Query.class, rendered);
        assertTrue(reparsed.ok(), reparsed::errorMessage);
        assertEquals(transformed, reparsed.value());
        var insert = parser.parse(Statement.class, "INSERT INTO forecasts (sales) " + rendered);
        assertTrue(insert.ok(), insert::errorMessage);
        assertEquals(insert.value(), parser.parse(Statement.class, renderer.render(insert.value()).sql()).value());
    }

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
