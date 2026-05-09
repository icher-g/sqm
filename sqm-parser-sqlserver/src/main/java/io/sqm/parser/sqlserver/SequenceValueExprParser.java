package io.sqm.parser.sqlserver;

import io.sqm.core.SequenceValueExpr;
import io.sqm.core.SequenceValueKind;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.Token;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.ok;

/**
 * SQL Server parser for {@code NEXT VALUE FOR sequence}.
 */
public class SequenceValueExprParser extends io.sqm.parser.ansi.SequenceValueExprParser {
    /**
     * Creates a SQL Server sequence value expression parser.
     */
    public SequenceValueExprParser() {
    }

    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.NEXT)
            && cur.match(this::isValueKeyword, 1)
            && cur.match(TokenType.FOR, 2)
            && cur.match(TokenType.IDENT, 3);
    }

    @Override
    public ParseResult<? extends SequenceValueExpr> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected NEXT", TokenType.NEXT);
        cur.expect("Expected VALUE", this::isValueKeyword);
        cur.expect("Expected FOR", TokenType.FOR);
        return ok(SequenceValueExpr.of(parseQualifiedName(cur), SequenceValueKind.NEXT_VALUE));
    }

    private boolean isValueKeyword(Token token) {
        return token.type() == TokenType.IDENT && "value".equalsIgnoreCase(token.lexeme());
    }
}
