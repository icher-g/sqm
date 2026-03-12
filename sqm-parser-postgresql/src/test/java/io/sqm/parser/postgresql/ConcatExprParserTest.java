package io.sqm.parser.postgresql;

import io.sqm.core.ColumnExpr;
import io.sqm.core.ConcatExpr;
import io.sqm.core.Expression;
import io.sqm.core.LiteralExpr;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcatExprParserTest {

    @Test
    void parsesDoublePipeConcatAsSemanticNode() {
        var ctx = ParseContext.of(new PostgresSpecs());
        var result = ctx.parse(Expression.class, "first_name || ' ' || last_name");

        assertTrue(result.ok(), result.errorMessage());

        var expr = assertInstanceOf(ConcatExpr.class, result.value());
        assertEquals(3, expr.args().size());
        assertEquals("first_name", assertInstanceOf(ColumnExpr.class, expr.args().getFirst()).name().value());
        assertEquals(" ", assertInstanceOf(LiteralExpr.class, expr.args().get(1)).value());
        assertEquals("last_name", assertInstanceOf(ColumnExpr.class, expr.args().get(2)).name().value());
    }
}
