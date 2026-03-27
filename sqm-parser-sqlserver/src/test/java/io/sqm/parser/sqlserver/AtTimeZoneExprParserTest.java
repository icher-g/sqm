package io.sqm.parser.sqlserver;

import io.sqm.core.AtTimeZoneExpr;
import io.sqm.core.Expression;
import io.sqm.core.LiteralExpr;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtTimeZoneExprParserTest {

    @Test
    void parsesAtTimeZoneForSupportedSqlServerVersions() {
        var context = ParseContext.of(new SqlServerSpecs(SqlDialectVersion.of(2019, 0)));

        var result = context.parse(Expression.class, "[created_at] AT TIME ZONE 'UTC'");

        assertTrue(result.ok(), result.errorMessage());
        var expr = assertInstanceOf(AtTimeZoneExpr.class, result.value());
        var timezone = assertInstanceOf(LiteralExpr.class, expr.timezone());
        assertEquals("UTC", timezone.value());
    }

    @Test
    void rejectsAtTimeZoneForUnsupportedSqlServerVersions() {
        var context = ParseContext.of(new SqlServerSpecs(SqlDialectVersion.of(2014, 0)));

        var result = context.parse(Expression.class, "[created_at] AT TIME ZONE 'UTC'");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("AT TIME ZONE"));
    }
}