package io.sqm.parser.ansi;

import io.sqm.core.ExprSelectItem;
import io.sqm.core.FunctionExpr;
import io.sqm.core.Query;
import io.sqm.core.SequenceValueExpr;
import io.sqm.core.SelectQuery;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SequenceValueExprParserTest {
    private final ParseContext ctx = ParseContext.of(new AnsiSpecs());

    @Test
    void doesNotMatchSequenceLikeSyntaxInAnsiExpressions() {
        assertTrue(ctx.parse(Query.class, "SELECT NEXT VALUE FOR users_seq").isError());

        var result = ctx.parse(Query.class, "SELECT nextval('users_seq')");

        assertTrue(result.ok(), result.errorMessage());
        var select = assertInstanceOf(SelectQuery.class, result.value());
        var item = assertInstanceOf(ExprSelectItem.class, select.items().getFirst());
        assertInstanceOf(FunctionExpr.class, item.expr());
        var cur = Cursor.of("nextval('users_seq')", ctx.identifierQuoting());
        assertFalse(ctx.parseIfMatch(SequenceValueExpr.class, cur).match());
    }
}
