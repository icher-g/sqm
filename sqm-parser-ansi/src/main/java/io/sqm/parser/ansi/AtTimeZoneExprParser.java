package io.sqm.parser.ansi;

import io.sqm.core.AtTimeZoneExpr;
import io.sqm.core.Expression;
import io.sqm.core.dialect.SqlFeature;
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
 * Parser for PostgreSQL {@code AT TIME ZONE} expression.
 * <p>
 * AT TIME ZONE is not part of ANSI SQL. This parser validates that the feature is supported
 * by the target dialect before attempting to parse the syntax. If the feature is not supported,
 * it returns a clear error message.
 * <p>
 * Syntax: {@code <timestamp_expr> AT TIME ZONE <timezone_expr>}
 */
public class AtTimeZoneExprParser implements MatchableParser<AtTimeZoneExpr>, InfixParser<Expression, AtTimeZoneExpr> {

    private final AtomicExprParser atomicParser;

    /**
     * Creates an AT TIME ZONE parser.
     *
     * @param atomicParser parser for atomic expressions
     */
    public AtTimeZoneExprParser(AtomicExprParser atomicParser) {
        this.atomicParser = atomicParser;
    }

    /**
     * Parses the AT TIME ZONE expression as a standalone expression.
     * AT TIME ZONE is a postfix-only operator, so this always returns an error.
     *
     * @param cur a Cursor instance containing tokens to parse
     * @param ctx a parser context containing parsers and lookups
     * @return error result indicating AT TIME ZONE is not supported as a standalone expression
     */
    @Override
    public ParseResult<? extends AtTimeZoneExpr> parse(Cursor cur, ParseContext ctx) {
        if (!ctx.capabilities().supports(SqlFeature.AT_TIME_ZONE)) {
            return error("AT TIME ZONE is not supported by this dialect", cur.fullPos());
        }
        var left = atomicParser.parse(cur, ctx);
        if (left.isError()) {
            return error(left);
        }
        return parse(left.value(), cur, ctx);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return the entity type to be handled by the handler
     */
    @Override
    public Class<? extends AtTimeZoneExpr> targetType() {
        return AtTimeZoneExpr.class;
    }

    /**
     * Performs a look-ahead test to determine whether this parser is applicable
     * at the current cursor position.
     * <p>
     * Checks for the keyword sequence: AT TIME ZONE
     * Since these are identifiers (not keywords), we check the lexeme values.
     *
     * @param cur the cursor pointing at the current token
     * @param ctx the parsing context
     * @return true if the next three tokens are AT, TIME, ZONE (case-insensitive)
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.IDENT, "at")
            && cur.match(TokenType.IDENT, "time", 1)
            && cur.match(TokenType.IDENT, "zone", 2);
    }

    /**
     * Parses AT TIME ZONE expression after a left-hand side operand.
     * <p>
     * Syntax: {@code <timestamp_expr> AT TIME ZONE <timezone_expr>}
     * <p>
     * First validates that the AT_TIME_ZONE feature is supported by the dialect.
     * If supported, parses the timezone expression and creates the AtTimeZoneExpr node.
     *
     * @param lhs the already parsed left-hand operand (the timestamp expression)
     * @param cur the cursor positioned at the AT token
     * @param ctx the parse context
     * @return result containing the parsed AtTimeZoneExpr, or an error if feature is unsupported
     */
    @Override
    public ParseResult<AtTimeZoneExpr> parse(Expression lhs, Cursor cur, ParseContext ctx) {
        // Check if the feature is supported by the current dialect
        if (!ctx.capabilities().supports(SqlFeature.AT_TIME_ZONE)) {
            return error("AT TIME ZONE is not supported by this dialect", cur.fullPos());
        }

        var t = cur.expect("Expected identifier", TokenType.IDENT);
        if (!t.lexeme().equalsIgnoreCase("at")) {
            return error("Expected AT", cur.fullPos());
        }

        t = cur.expect("Expected identifier", TokenType.IDENT);
        if (!t.lexeme().equalsIgnoreCase("time")) {
            return error("Expected TIME after AT", cur.fullPos());
        }

        t = cur.expect("Expected identifier", TokenType.IDENT);
        if (!t.lexeme().equalsIgnoreCase("zone")) {
            return error("Expected ZONE after AT TIME", cur.fullPos());
        }

        // Parse the timezone expression
        var timezoneResult = ctx.parse(Expression.class, cur);
        if (timezoneResult.isError()) {
            return error(timezoneResult);
        }

        return ok(AtTimeZoneExpr.of(lhs, timezoneResult.value()));
    }
}

