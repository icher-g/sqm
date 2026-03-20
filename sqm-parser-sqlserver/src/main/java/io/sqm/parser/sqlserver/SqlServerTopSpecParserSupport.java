package io.sqm.parser.sqlserver;

import io.sqm.core.Expression;
import io.sqm.core.TopSpec;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

final class SqlServerTopSpecParserSupport {
    private SqlServerTopSpecParserSupport() {
    }

    static ParseResult<TopSpec> parseTopClause(Cursor cur, ParseContext ctx, boolean allowWithTies, String unsupportedWithTiesMessage) {
        if (!cur.consumeIf(TokenType.TOP)) {
            return ok(null);
        }

        var parenthesized = cur.consumeIf(TokenType.LPAREN);
        var expr = ctx.parse(Expression.class, cur);
        if (expr.isError()) {
            return error(expr);
        }
        var count = expr.value();
        if (parenthesized) {
            cur.expect("Expected ')' to close TOP expression", TokenType.RPAREN);
        }

        var percent = cur.consumeIf(TokenType.PERCENT);

        boolean withTies = false;
        if (cur.consumeIf(TokenType.WITH)) {
            cur.expect("Expected TIES after WITH in TOP clause", TokenType.TIES);
            withTies = true;
        }

        if (withTies && !allowWithTies) {
            return error(unsupportedWithTiesMessage, cur.fullPos());
        }

        return ok(TopSpec.of(count, percent, withTies));
    }
}
