package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.MultiplicativeArithmeticExpr;
import io.sqm.core.SubArithmeticExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.InfixParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.core.OperatorTokens.isMinus;
import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses binary subtraction expressions.
 */
public class SubArithmeticExprParser implements Parser<SubArithmeticExpr>, InfixParser<Expression, SubArithmeticExpr> {
    /**
     * Creates a subtraction expression parser.
     */
    public SubArithmeticExprParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<SubArithmeticExpr> parse(Cursor cur, ParseContext ctx) {
        var lhs = ctx.parseEnclosed(MultiplicativeArithmeticExpr.class, cur);
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
    public ParseResult<SubArithmeticExpr> parse(Expression lhs, Cursor cur, ParseContext ctx) {
        cur.expect("Expected -", t -> isMinus(t));

        var rhs = ctx.parseEnclosed(MultiplicativeArithmeticExpr.class, cur);
        if (rhs.isError()) {
            return error(rhs);
        }
        return ok(SubArithmeticExpr.of(lhs, rhs.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<SubArithmeticExpr> targetType() {
        return SubArithmeticExpr.class;
    }
}
