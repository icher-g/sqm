package io.sqm.control;

import io.sqm.core.AnonymousParamExpr;
import io.sqm.core.Statement;
import io.sqm.core.collect.ParametersCollector;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.mysql.spi.MySqlSpecs;
import io.sqm.parser.oracle.spi.OracleSpecs;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.mysql.spi.MySqlDialect;
import io.sqm.render.oracle.spi.OracleDialect;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.sqlserver.spi.SqlServerDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompactJdbcParameterTest {
    @Test
    void explicitPostgresQuestionMarkOperatorRoundTripsWithOneParameter() {
        var parser = ParseContext.of(new PostgresSpecs());
        var statement = parse(parser, "SELECT data OPERATOR(custom.=?) ? FROM users");
        assertParameters(statement, 1);
        var sql = RenderContext.of(new PostgresDialect()).render(statement).sql();
        assertEquals(statement, parse(parser, sql));
    }

    @Test
    void compactParametersRoundTripAcrossDialects() {
        check(ParseContext.of(new AnsiSpecs()), RenderContext.of(new AnsiDialect()));
        check(ParseContext.of(new PostgresSpecs()), RenderContext.of(new PostgresDialect()));
        check(ParseContext.of(new MySqlSpecs()), RenderContext.of(new MySqlDialect()));
        check(ParseContext.of(new SqlServerSpecs()), RenderContext.of(new SqlServerDialect()));
        check(ParseContext.of(new OracleSpecs()), RenderContext.of(new OracleDialect()));
    }

    private void check(ParseContext parser, RenderContext renderer) {
        for (var operator : new String[]{"=", ">=", "<=", "<>", "!=", ">", "<"}) {
            var prefix = "SELECT u.id FROM users u WHERE u.tenant_id";
            var compact = parse(parser, prefix + operator + "? ORDER BY u.id");
            var spaced = parse(parser, prefix + " " + operator + " ? ORDER BY u.id");
            assertEquals(spaced, compact);
            assertParameters(compact, 1);
            assertEquals(compact, parse(parser, renderer.render(compact).sql()));
        }
        for (var sql : new String[]{
            "UPDATE users SET tenant_id=? WHERE id>=?",
            "DELETE FROM users WHERE tenant_id=? AND id<>?",
            "SELECT u.id FROM users u JOIN users v ON v.id=u.id WHERE u.tenant_id=? AND v.id<=? ORDER BY u.id"}) {
            var statement = parse(parser, sql);
            assertParameters(statement, 2);
            assertEquals(statement, parse(parser, renderer.render(statement).sql()));
        }
    }

    private Statement parse(ParseContext parser, String sql) {
        var result = parser.parse(Statement.class, sql);
        assertTrue(result.ok(), result::errorMessage);
        return result.value();
    }

    private void assertParameters(Statement statement, int count) {
        var collector = new ParametersCollector();
        statement.accept(collector);
        assertEquals(count, collector.positional().size());
        assertTrue(collector.named().isEmpty());
        collector.positional().forEach(parameter -> assertInstanceOf(AnonymousParamExpr.class, parameter));
    }
}
