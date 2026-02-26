package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.PowerArithmeticExpr;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.PostfixExprParser;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.Objects;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses exponentiation expressions.
 */
public class PowerArithmeticExprParser implements Parser<Expression> {

    private final PostfixExprParser postfixExprParser;

    /**
     * Creates an exponentiation parser.
     *
     * @param postfixExprParser parser for postfix expressions
     */
    public PowerArithmeticExprParser(PostfixExprParser postfixExprParser) {
        this.postfixExprParser = postfixExprParser;
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends Expression> parse(Cursor cur, ParseContext ctx) {
        var lhs = postfixExprParser.parse(cur, ctx);
        if (lhs.isError()) {
            return lhs;
        }

        while (cur.consumeIf(t -> t.type() == TokenType.OPERATOR && Objects.equals(t.lexeme(), "^"))) {
            if (!ctx.capabilities().supports(SqlFeature.EXPONENTIATION_OPERATOR)) {
                return error("^ operator is not supported by this dialect", cur.fullPos());
            }
            var rhs = postfixExprParser.parse(cur, ctx);
            if (rhs.isError()) {
                return error(rhs);
            }
            lhs = ok(PowerArithmeticExpr.of(lhs.value(), rhs.value()));
        }
        return lhs;
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends PowerArithmeticExpr> targetType() {
        return PowerArithmeticExpr.class;
    }
}
