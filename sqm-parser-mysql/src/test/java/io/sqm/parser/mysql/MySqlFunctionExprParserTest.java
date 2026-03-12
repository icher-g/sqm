package io.sqm.parser.mysql;

import io.sqm.core.ColumnExpr;
import io.sqm.core.ConcatExpr;
import io.sqm.core.Expression;
import io.sqm.core.FunctionExpr;
import io.sqm.core.IntervalLiteralExpr;
import io.sqm.core.LiteralExpr;
import io.sqm.parser.mysql.spi.MySqlSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySqlFunctionExprParserTest {

    @Test
    void parsesJsonExtractFunction() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(FunctionExpr.class, "JSON_EXTRACT(payload, '$.user.id')");

        assertTrue(result.ok(), result.errorMessage());
        var function = result.value();
        assertEquals("JSON_EXTRACT", function.name().values().getLast());
        assertEquals(2, function.args().size());
        assertInstanceOf(ColumnExpr.class, ((FunctionExpr.Arg.ExprArg) function.args().get(0)).expr());
        var path = assertInstanceOf(LiteralExpr.class, ((FunctionExpr.Arg.ExprArg) function.args().get(1)).expr());
        assertEquals("$.user.id", path.value());
    }

    @Test
    void parsesJsonObjectFunction() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(FunctionExpr.class, "JSON_OBJECT('id', user_id, 'name', user_name)");

        assertTrue(result.ok(), result.errorMessage());
        var function = result.value();
        assertEquals("JSON_OBJECT", function.name().values().getLast());
        assertEquals(4, function.args().size());
        assertEquals("id", assertInstanceOf(LiteralExpr.class, ((FunctionExpr.Arg.ExprArg) function.args().get(0)).expr()).value());
        assertInstanceOf(ColumnExpr.class, ((FunctionExpr.Arg.ExprArg) function.args().get(1)).expr());
    }

    @Test
    void parsesDateAddWithIntervalLiteralArgument() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(FunctionExpr.class, "DATE_ADD(created_at, INTERVAL '1' DAY)");

        assertTrue(result.ok(), result.errorMessage());
        var function = result.value();
        assertEquals("DATE_ADD", function.name().values().getLast());
        assertEquals(2, function.args().size());
        assertInstanceOf(ColumnExpr.class, ((FunctionExpr.Arg.ExprArg) function.args().getFirst()).expr());
        var interval = assertInstanceOf(IntervalLiteralExpr.class, ((FunctionExpr.Arg.ExprArg) function.args().get(1)).expr());
        assertEquals("1", interval.value());
        assertEquals("DAY", interval.qualifier().orElseThrow());
    }

    @Test
    void parsesDateAddWithUnquotedIntervalLiteralArgument() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(FunctionExpr.class, "DATE_ADD(created_at, INTERVAL 1 DAY)");

        assertTrue(result.ok(), result.errorMessage());
        var function = result.value();
        var interval = assertInstanceOf(IntervalLiteralExpr.class, ((FunctionExpr.Arg.ExprArg) function.args().get(1)).expr());
        assertEquals("1", interval.value());
        assertEquals("DAY", interval.qualifier().orElseThrow());
    }

    @Test
    void parsesDateSubWithSignedUnquotedIntervalLiteralArgument() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(FunctionExpr.class, "DATE_SUB(created_at, INTERVAL -1 DAY)");

        assertTrue(result.ok(), result.errorMessage());
        var function = result.value();
        var interval = assertInstanceOf(IntervalLiteralExpr.class, ((FunctionExpr.Arg.ExprArg) function.args().get(1)).expr());
        assertEquals("-1", interval.value());
        assertEquals("DAY", interval.qualifier().orElseThrow());
    }

    @Test
    void parsesStringFunctions() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var concatResult = ctx.parse(Expression.class, "CONCAT(first_name, ' ', last_name)");
        var concatWsResult = ctx.parse(FunctionExpr.class, "CONCAT_WS('-', first_name, last_name)");
        var substringResult = ctx.parse(FunctionExpr.class, "SUBSTRING_INDEX(email, '@', 1)");

        assertTrue(concatResult.ok(), concatResult.errorMessage());
        assertTrue(concatWsResult.ok(), concatWsResult.errorMessage());
        assertTrue(substringResult.ok(), substringResult.errorMessage());

        var concatExpr = assertInstanceOf(ConcatExpr.class, concatResult.value());
        assertEquals(3, concatExpr.args().size());
        assertEquals("CONCAT_WS", concatWsResult.value().name().values().getLast());
        assertEquals("SUBSTRING_INDEX", substringResult.value().name().values().getLast());
    }

    @Test
    void parseConcatRequiresAtLeastOneArgument() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(Expression.class, "CONCAT()");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).startsWith("CONCAT requires at least one argument"));
    }

    @Test
    void nonConcatExpressionsFallBackToRegularExpressionParsing() {
        var ctx = ParseContext.of(new MySqlSpecs());
        var result = ctx.parse(Expression.class, "first_name");

        assertTrue(result.ok(), result.errorMessage());
        assertInstanceOf(ColumnExpr.class, result.value());
    }
}
