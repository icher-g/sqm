package io.sqm.core.match;

import io.sqm.core.*;
import org.junit.jupiter.api.Test;

import static io.sqm.core.Expression.funcArg;
import static io.sqm.core.Expression.literal;
import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;


public class ExpressionMatchTest {

    @Test
    void kase_forSpecificSubtype_isChosen() {
        var col = ColumnExpr.of("c");
        var out = Match
            .<String>expression(col)
            .func(f -> "FUNC")
            .column(c -> "COL")
            .otherwise(e -> "OTHER");
        assertEquals("COL", out);
    }

    @Test
    void multiple_kases_firstMatchWins() {
        var kase = kase(when(col("u", "name").gt(10)).then(col("o", "name")));
        String out = ExpressionMatch
            .<String>match(kase)
            .func(f -> "FUNC")
            .kase(c -> "CASE")
            .otherwise(e -> "OTHER");
        assertEquals("CASE", out);
    }

    @Test
    void otherwise_variants_work() {
        LiteralExpr lit = lit(10);

        String v1 = ExpressionMatch
            .<String>match(lit)
            .kase(c -> "NOPE")
            .orElse("DEF");
        assertEquals("DEF", v1);

        String v2 = ExpressionMatch
            .<String>match(lit)
            .kase(c -> "NOPE")
            .orElseGet(() -> "SUP");
        assertEquals("SUP", v2);

        assertThrows(IllegalStateException.class, () ->
            ExpressionMatch
                .<String>match(lit)
                .kase(c -> "NOPE")
                .orElseThrow(IllegalStateException::new)
        );

        assertTrue(ExpressionMatch
            .<String>match(lit)
            .kase(c -> "NOPE")
            .otherwiseEmpty()
            .isEmpty());
    }

    @Test
    void matches_funcArg() {
        var expr = funcArg(literal(1));
        String out = Match
            .<String>expression(expr)
            .valueSet(v -> "V")
            .predicate(p -> "P")
            .funcArg(a -> "FA")
            .column(c -> "C")
            .literal(l -> "L")
            .func(f -> "F")
            .kase(k -> "K")
            .orElseGet(() -> "ELSE");

        assertEquals("FA", out);
    }

    @Test
    void matches_literal() {
        var expr = literal(1);
        String out = Match
            .<String>expression(expr)
            .column(c -> "C")
            .func(f -> "F")
            .kase(k -> "K")
            .literal(l -> "L")
            .valueSet(v -> "V")
            .predicate(p -> "P")
            .funcArg(a -> "FA")
            .orElseGet(() -> "ELSE");

        assertEquals("L", out);
    }

    @Test
    void matches_typed_literals() {
        String dateResult = Match
            .<String>expression(DateLiteralExpr.of("2020-01-01"))
            .dateLiteral(d -> "DATE")
            .literal(l -> "L")
            .orElse("ELSE");
        assertEquals("DATE", dateResult);

        String timeResult = Match
            .<String>expression(TimeLiteralExpr.of("10:11:12", TimeZoneSpec.WITH_TIME_ZONE))
            .timeLiteral(t -> "TIME")
            .literal(l -> "L")
            .orElse("ELSE");
        assertEquals("TIME", timeResult);

        String tsResult = Match
            .<String>expression(TimestampLiteralExpr.of("2020-01-01 00:00:00"))
            .timestampLiteral(t -> "TS")
            .literal(l -> "L")
            .orElse("ELSE");
        assertEquals("TS", tsResult);

        String intervalResult = Match
            .<String>expression(IntervalLiteralExpr.of("1", "DAY"))
            .intervalLiteral(i -> "INT")
            .literal(l -> "L")
            .orElse("ELSE");
        assertEquals("INT", intervalResult);

        String bitResult = Match
            .<String>expression(BitStringLiteralExpr.of("1010"))
            .bitStringLiteral(b -> "BIT")
            .literal(l -> "L")
            .orElse("ELSE");
        assertEquals("BIT", bitResult);

        String hexResult = Match
            .<String>expression(HexStringLiteralExpr.of("FF"))
            .hexStringLiteral(h -> "HEX")
            .literal(l -> "L")
            .orElse("ELSE");
        assertEquals("HEX", hexResult);

        String escapeResult = Match
            .<String>expression(EscapeStringLiteralExpr.of("it\\'s"))
            .escapeStringLiteral(e -> "ESC")
            .literal(l -> "L")
            .orElse("ELSE");
        assertEquals("ESC", escapeResult);

        String dollarResult = Match
            .<String>expression(DollarStringLiteralExpr.of("tag", "value"))
            .dollarStringLiteral(d -> "DOLLAR")
            .literal(l -> "L")
            .orElse("ELSE");
        assertEquals("DOLLAR", dollarResult);
    }

    @Test
    void matches_row() {
        var expr = row(1, 2, 3);
        String out = Match
            .<String>valueSet(expr)
            .rows(r -> "RS")
            .query(q -> "Q")
            .row(r -> "R")
            .orElseGet(() -> "ELSE");

        assertEquals("R", out);
    }

    @Test
    void matches_rows() {
        var expr = rows(row(1, 2, 3));
        String out = Match
            .<String>valueSet(expr)
            .query(q -> "Q")
            .row(r -> "R")
            .rows(r -> "RS")
            .orElse("ELSE");

        assertEquals("RS", out);
    }

    @Test
    void matches_query() {
        var expr = expr(Query.select(literal(1)));
        String out = Match
            .<String>valueSet(expr)
            .row(r -> "R")
            .rows(r -> "RS")
            .query(q -> "Q")
            .orElse("ELSE");

        assertEquals("Q", out);
    }

    @Test
    void matches_func() {
        var expr = func("n");
        String out = Match
            .<String>expression(expr)
            .column(c -> "C")
            .kase(k -> "K")
            .funcArg(a -> "FA")
            .literal(l -> "L")
            .func(f -> "F")
            .orElse("ELSE");

        assertEquals("F", out);
    }

    @Test
    void matches_kase() {
        var expr = kase(when(col("u", "name").gt(10)).then(col("o", "name")));
        String out = Match
            .<String>expression(expr)
            .funcArg(a -> "FA")
            .literal(l -> "L")
            .func(f -> "F")
            .kase(k -> "K")
            .column(c -> "C")
            .orElseThrow(() -> new IllegalArgumentException(""));

        assertEquals("K", out);
    }

    @Test
    void match_param() {
        var expr = param();
        String out = Match
            .<String>expression(expr)
            .literal(l -> "L")
            .func(f -> "F")
            .param(p -> p.<String>matchParam()
                .anonymous(a -> "A")
                .named(n -> "N")
                .ordinal(o -> "O")
                .orElse("ELSE")
            )
            .funcArg(a -> "FA")
            .kase(k -> "K")
            .column(c -> "C")
            .orElse("No a param");

        assertEquals("A", out);
    }

    @Test
    void match_valueSet() {
        var expr = row(1, 2);
        String out = Match
            .<String>expression(expr)
            .valueSet(v -> "V")
            .predicate(p -> "P")
            .orElse("ELSE");

        assertEquals("V", out);
    }

    @Test
    void match_predicate() {
        var expr = col("c").eq(1);
        String out = Match
            .<String>expression(expr)
            .valueSet(v -> "V")
            .predicate(p -> "P")
            .orElse("ELSE");

        assertEquals("P", out);
    }
}
