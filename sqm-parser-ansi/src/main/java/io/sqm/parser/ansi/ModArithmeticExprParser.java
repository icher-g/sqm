package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.ModArithmeticExpr;
import io.sqm.parser.AtomicExprParser;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.ParserException;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.InfixParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

public class ModArithmeticExprParser implements Parser<ModArithmeticExpr>, InfixParser<Expression, ModArithmeticExpr> {

    private final AtomicExprParser atomicExprParser;

    public ModArithmeticExprParser(AtomicExprParser atomicExprParser) {
        this.atomicExprParser = atomicExprParser;
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<ModArithmeticExpr> parse(Cursor cur, ParseContext ctx) {
        var t = cur.expect("Expected IDENTIFIER", TokenType.IDENT);
        if (!t.lexeme().equalsIgnoreCase("MOD")) {
            throw new ParserException("Expected MOD", cur.fullPos());
        }

        cur.expect("Expected (", TokenType.LPAREN);

        var lhs = atomicExprParser.parse(cur, ctx);
        if (lhs.isError()) {
            return error(lhs);
        }

        cur.expect("Expected ,", TokenType.COMMA);

        var rhs = atomicExprParser.parse(cur, ctx);
        if (rhs.isError()) {
            return error(rhs);
        }

        cur.expect("Expected )", TokenType.RPAREN);
        return ok(ModArithmeticExpr.of(lhs.value(), rhs.value()));
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
    public ParseResult<ModArithmeticExpr> parse(Expression lhs, Cursor cur, ParseContext ctx) {
        cur.expect("Expected %", TokenType.PERCENT);

        var rhs = atomicExprParser.parse(cur, ctx);
        if (rhs.isError()) {
            return error(rhs);
        }
        return ok(ModArithmeticExpr.of(lhs, rhs.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<ModArithmeticExpr> targetType() {
        return ModArithmeticExpr.class;
    }
}
