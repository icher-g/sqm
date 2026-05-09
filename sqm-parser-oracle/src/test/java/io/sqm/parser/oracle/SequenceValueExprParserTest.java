package io.sqm.parser.oracle;

import io.sqm.core.ExprSelectItem;
import io.sqm.core.Query;
import io.sqm.core.SequenceValueExpr;
import io.sqm.core.SequenceValueKind;
import io.sqm.core.SelectQuery;
import io.sqm.parser.oracle.spi.OracleSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SequenceValueExprParserTest {
    private final ParseContext ctx = ParseContext.of(new OracleSpecs());

    @Test
    void parsesNextvalAndCurrvalDotSyntax() {
        var next = ctx.parse(Query.class, "SELECT users_seq.NEXTVAL FROM dual");
        var current = ctx.parse(Query.class, "SELECT app.users_seq.CURRVAL FROM dual");

        assertTrue(next.ok(), next.errorMessage());
        assertTrue(current.ok(), current.errorMessage());
        assertSequence(firstExpression(next.value()), SequenceValueKind.NEXT_VALUE, "users_seq");
        assertSequence(firstExpression(current.value()), SequenceValueKind.CURRENT_VALUE, "app", "users_seq");
    }

    @Test
    void leavesFunctionStyleAsUnsupportedOracleFunctionParsing() {
        var result = ctx.parse(SequenceValueExpr.class, "nextval('users_seq')");

        assertTrue(result.isError());
    }

    private static SequenceValueExpr firstExpression(Query query) {
        var select = assertInstanceOf(SelectQuery.class, query);
        var item = assertInstanceOf(ExprSelectItem.class, select.items().getFirst());
        return assertInstanceOf(SequenceValueExpr.class, item.expr());
    }

    private static void assertSequence(SequenceValueExpr expr, SequenceValueKind kind, String... parts) {
        assertEquals(kind, expr.kind());
        assertEquals(java.util.List.of(parts), expr.sequence().values());
    }
}
