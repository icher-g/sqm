package io.sqm.parser.ansi;

import io.sqm.core.CollateExpr;
import io.sqm.core.Expression;
import io.sqm.core.QualifiedName;
import io.sqm.parser.AtomicExprParser;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.InfixParser;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses expression-level {@code COLLATE} constructs.
 */
public class CollateExprParser implements MatchableParser<Expression>, InfixParser<Expression, CollateExpr> {

    private final AtomicExprParser atomicParser;

    /**
     * Creates a collate expression parser.
     *
     * @param atomicParser parser for the base (atomic) expression part.
     */
    public CollateExprParser(AtomicExprParser atomicParser) {
        this.atomicParser = atomicParser;
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
        if (!ctx.capabilities().supports(io.sqm.core.dialect.SqlFeature.EXPR_COLLATE)) {
            return error("COLLATE is not supported by this dialect", cur.fullPos());
        }
        var left = atomicParser.parse(cur, ctx);
        if (left.isError()) {
            return left;
        }
        return parse(left.value(), cur, ctx);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends CollateExpr> targetType() {
        return CollateExpr.class;
    }

    /**
     * Performs a look-ahead test to determine whether this parser is applicable
     * at the current cursor position.
     *
     * @param cur the cursor pointing at the current token
     * @param ctx the parsing context providing configuration and utilities
     * @return {@code true} if this parser should be used to parse the upcoming input
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        if (!ctx.capabilities().supports(io.sqm.core.dialect.SqlFeature.EXPR_COLLATE)) {
            return false;
        }
        return cur.match(TokenType.COLLATE);
    }

    /**
     * Parses a COLLATE occurrence where the left-hand side operand
     * has already been parsed.
     *
     * @param lhs the already parsed left-hand operand
     * @param cur the cursor positioned at the COLLATE token
     * @param ctx the parse context
     * @return the parsing result representing {@code lhs COLLATE collation}
     */
    @Override
    public ParseResult<CollateExpr> parse(Expression lhs, Cursor cur, ParseContext ctx) {
        if (!ctx.capabilities().supports(io.sqm.core.dialect.SqlFeature.EXPR_COLLATE)) {
            return error("COLLATE is not supported by this dialect", cur.fullPos());
        }
        if (lhs instanceof CollateExpr) {
            return error("COLLATE specified more than once", cur.fullPos());
        }
        cur.expect("Expected COLLATE", TokenType.COLLATE);
        var collation = parseCollationName(cur);
        if (collation == null) {
            return error("Expected collation name after COLLATE", cur.fullPos());
        }
        return ok(CollateExpr.of(lhs, collation));
    }

    private QualifiedName parseCollationName(Cursor cur) {
        if (!cur.match(TokenType.IDENT)) {
            return null;
        }
        return parseQualifiedName(cur);
    }
}
