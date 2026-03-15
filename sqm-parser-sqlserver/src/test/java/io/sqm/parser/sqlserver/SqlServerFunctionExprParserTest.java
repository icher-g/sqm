package io.sqm.parser.sqlserver;

import io.sqm.core.FunctionExpr;
import io.sqm.core.LiteralExpr;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlServerFunctionExprParserTest {

    @Test
    void parsesFirstWaveSqlServerFunctions() {
        var ctx = ParseContext.of(new SqlServerSpecs());

        var len = ctx.parse(FunctionExpr.class, "LEN(name)");
        var dataLength = ctx.parse(FunctionExpr.class, "DATALENGTH(payload)");
        var getDate = ctx.parse(FunctionExpr.class, "GETDATE()");
        var isNull = ctx.parse(FunctionExpr.class, "ISNULL(name, 'unknown')");
        var stringAgg = ctx.parse(FunctionExpr.class, "STRING_AGG(name, ',') WITHIN GROUP (ORDER BY name)");

        assertTrue(len.ok(), len.errorMessage());
        assertTrue(dataLength.ok(), dataLength.errorMessage());
        assertTrue(getDate.ok(), getDate.errorMessage());
        assertTrue(isNull.ok(), isNull.errorMessage());
        assertTrue(stringAgg.ok(), stringAgg.errorMessage());
        assertEquals("LEN", len.value().name().values().getLast());
        assertEquals("DATALENGTH", dataLength.value().name().values().getLast());
        assertEquals("GETDATE", getDate.value().name().values().getLast());
        assertEquals("ISNULL", isNull.value().name().values().getLast());
        assertEquals("STRING_AGG", stringAgg.value().name().values().getLast());
        assertEquals(0, getDate.value().args().size());
        assertEquals(2, stringAgg.value().args().size());
        assertNotNull(stringAgg.value().withinGroup());
    }

    @Test
    void parsesDateAddAndDateDiffDatepartsAsStringLiterals() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var dateAdd = ctx.parse(FunctionExpr.class, "DATEADD(day, 1, created_at)");
        var dateDiff = ctx.parse(FunctionExpr.class, "DATEDIFF(day, start_at, end_at)");

        assertTrue(dateAdd.ok(), dateAdd.errorMessage());
        assertTrue(dateDiff.ok(), dateDiff.errorMessage());

        var dateAddArg = assertInstanceOf(FunctionExpr.Arg.ExprArg.class, dateAdd.value().args().getFirst());
        var dateDiffArg = assertInstanceOf(FunctionExpr.Arg.ExprArg.class, dateDiff.value().args().getFirst());
        assertEquals("day", assertInstanceOf(LiteralExpr.class, dateAddArg.expr()).value());
        assertEquals("day", assertInstanceOf(LiteralExpr.class, dateDiffArg.expr()).value());
    }
}
