package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.UnaryOperatorExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

public class UnaryOperatorExprParser implements Parser<UnaryOperatorExpr> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends UnaryOperatorExpr> parse(Cursor cur, ParseContext ctx) {
        var t = cur.expect("Expected operator", TokenType.OPERATOR);
        var result = ctx.parseEnclosed(Expression.class, cur);
        if (result.isError()) {
            return error(result);
        }
        return ok(UnaryOperatorExpr.of(t.lexeme(), result.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends UnaryOperatorExpr> targetType() {
        return UnaryOperatorExpr.class;
    }
}
