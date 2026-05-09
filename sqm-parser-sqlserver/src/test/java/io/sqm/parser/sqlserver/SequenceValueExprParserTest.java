package io.sqm.parser.sqlserver;

import io.sqm.core.ExprSelectItem;
import io.sqm.core.Query;
import io.sqm.core.SequenceValueExpr;
import io.sqm.core.SequenceValueKind;
import io.sqm.core.SelectQuery;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SequenceValueExprParserTest {
    private final ParseContext ctx = ParseContext.of(new SqlServerSpecs());

    @Test
    void parsesNextValueFor() {
        var result = ctx.parse(Query.class, "SELECT NEXT VALUE FOR app.users_seq");

        assertTrue(result.ok(), result.errorMessage());
        var expr = firstExpression(result.value());
        assertEquals(SequenceValueKind.NEXT_VALUE, expr.kind());
        assertEquals(java.util.List.of("app", "users_seq"), expr.sequence().values());
    }

    @Test
    void rejectsCurrentValueFunctionStyle() {
        assertTrue(ctx.parse(SequenceValueExpr.class, "currval('users_seq')").isError());
    }

    @Test
    void matchRejectsIncompleteNextValueForSyntax() {
        var parser = new SequenceValueExprParser();

        assertFalse(parser.match(Cursor.of("CURRENT VALUE FOR users_seq", ctx.identifierQuoting()), ctx));
        assertFalse(parser.match(Cursor.of("NEXT FOR users_seq", ctx.identifierQuoting()), ctx));
        assertFalse(parser.match(Cursor.of("NEXT VALUE users_seq", ctx.identifierQuoting()), ctx));
        assertFalse(parser.match(Cursor.of("NEXT VALUE FOR", ctx.identifierQuoting()), ctx));
    }

    private static SequenceValueExpr firstExpression(Query query) {
        var select = assertInstanceOf(SelectQuery.class, query);
        var item = assertInstanceOf(ExprSelectItem.class, select.items().getFirst());
        return assertInstanceOf(SequenceValueExpr.class, item.expr());
    }
}
