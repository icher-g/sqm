package io.sqm.parser.postgresql;

import io.sqm.core.ExprSelectItem;
import io.sqm.core.FunctionExpr;
import io.sqm.core.Query;
import io.sqm.core.SequenceValueExpr;
import io.sqm.core.SequenceValueKind;
import io.sqm.core.SelectQuery;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SequenceValueExprParserTest {
    private final ParseContext ctx = ParseContext.of(new PostgresSpecs());

    @Test
    void parsesSimpleNextvalAndCurrvalLiteralCalls() {
        var next = ctx.parse(Query.class, "SELECT nextval('app.users_seq')");
        var current = ctx.parse(Query.class, "SELECT currval('users_seq')");

        assertTrue(next.ok(), next.errorMessage());
        assertTrue(current.ok(), current.errorMessage());
        assertSequence(firstExpression(next.value()), SequenceValueKind.NEXT_VALUE, "app", "users_seq");
        assertSequence(firstExpression(current.value()), SequenceValueKind.CURRENT_VALUE, "users_seq");
    }

    @Test
    void dynamicNextvalStaysFunctionExpression() {
        var result = ctx.parse(Query.class, "SELECT nextval(sequence_name)");

        assertTrue(result.ok(), result.errorMessage());
        var select = assertInstanceOf(SelectQuery.class, result.value());
        var item = assertInstanceOf(ExprSelectItem.class, select.items().getFirst());
        assertInstanceOf(FunctionExpr.class, item.expr());
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
