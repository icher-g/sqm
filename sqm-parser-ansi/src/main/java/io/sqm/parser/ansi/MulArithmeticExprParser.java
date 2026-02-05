package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.MulArithmeticExpr;
import io.sqm.core.PowerArithmeticExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.InfixParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.core.OperatorTokens.isStar;
import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

public class MulArithmeticExprParser implements Parser<MulArithmeticExpr>, InfixParser<Expression, MulArithmeticExpr> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<MulArithmeticExpr> parse(Cursor cur, ParseContext ctx) {
        var lhs = ctx.parse(PowerArithmeticExpr.class, cur);
        if (lhs.isError()) {
            return error(lhs);
        }
        return parse(lhs.value(), cur, ctx);
    }

    /**
     * Parses a binary operator occurrence where the left-hand side operand
     * has already been parsed.
     *
     * <p>The cursor is positioned at the operator token when this method
     * is invoked. Implementations are responsible for consuming the operator
     * token, parsing the right-hand side operand, and constructing the
     * resulting node.</p>
     *
     * @param lhs the already parsed left-hand operand
     * @param cur the cursor positioned at the operator token
     * @param ctx the parse context
     * @return the parsing result representing {@code lhs <op> rhs}
     */
    @Override
    public ParseResult<MulArithmeticExpr> parse(Expression lhs, Cursor cur, ParseContext ctx) {
        cur.expect("Expected *", t -> isStar(t));

        var rhs = ctx.parse(PowerArithmeticExpr.class, cur);
        if (rhs.isError()) {
            return error(rhs);
        }
        return ok(MulArithmeticExpr.of(lhs, rhs.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<MulArithmeticExpr> targetType() {
        return MulArithmeticExpr.class;
    }
}
