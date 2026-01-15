package io.sqm.parser;

import io.sqm.core.*;
import io.sqm.parser.core.*;
import io.sqm.parser.spi.MatchResult;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.core.OperatorTokens.isMinus;
import static io.sqm.parser.spi.ParseResult.error;

/**
 * Parses an *atomic expression* — the smallest indivisible expression unit
 * that can appear within arithmetic expressions, predicates, SELECT lists,
 * function arguments, row constructors, and other SQL constructs.
 *
 * <p>This parser intentionally handles only the highest-precedence elements of
 * SQL expression grammar. It does <em>not</em> deal with operators (such as
 * {@code +}, {@code -}, {@code *}, {@code /}) or other combinators. Those are
 * handled by higher-level arithmetic parsers that compose atomic expressions
 * into full expression trees.</p>
 *
 * <h2>Atomic expressions include:</h2>
 * <ul>
 *   <li>Unary expressions (e.g. {@code -x})</li>
 *   <li>Parenthesized expressions (e.g. {@code (a + b)})</li>
 *   <li>{@code CASE} expressions</li>
 *   <li>Function calls (e.g. {@code SUM(x)}, {@code COALESCE(a,b)})</li>
 *   <li>Positional / named parameters ({@code ?}, {@code :name}, {@code $1})</li>
 *   <li>Subquery expressions ({@link QueryExpr})</li>
 *   <li>Row constructors ({@link RowExpr})</li>
 *   <li>Column references ({@link ColumnExpr})</li>
 *   <li>Literals ({@link LiteralExpr})</li>
 * </ul>
 *
 * <p>If the next token does not match any supported atomic construct, an error
 * is returned indicating that the input cannot begin an expression.</p>
 *
 * <h2>Usage</h2>
 * <p>This parser is typically invoked by:</p>
 * <ul>
 *   <li>multiplicative arithmetic parser (for factors)</li>
 *   <li>predicate expression parser</li>
 *   <li>list parsers (SELECT items, row constructors, function arguments)</li>
 * </ul>
 *
 * <p>It never recurses into itself, except through parenthesized expressions.</p>
 */
public class AtomicExprParser {

    /**
     * Attempts to parse the next token(s) as an atomic SQL expression.
     *
     * <p>The method checks the leading token and delegates to the appropriate
     * specialized expression parser using {@link ParseContext#parseIfMatch}.
     * Expressions are attempted in well-defined precedence order:
     *
     * <ol>
     *   <li>Unary negation ({@code -x})</li>
     *   <li>Parenthesized expressions</li>
     *   <li>{@code CASE} expressions</li>
     *   <li>Function expressions</li>
     *   <li>Parameter expressions</li>
     *   <li>Subquery expressions</li>
     *   <li>Row constructors</li>
     *   <li>Column references</li>
     *   <li>Literals</li>
     * </ol>
     *
     * <p>If no atomic construct matches, an error is returned.</p>
     *
     * @param cur the cursor over the token stream
     * @param ctx the parser context used for dispatching and error reporting
     * @return a {@link ParseResult} containing the parsed atomic expression,
     * or an error result if parsing fails
     */
    public ParseResult<? extends Expression> parse(Cursor cur, ParseContext ctx) {
        if (isMinus(cur.peek())) {
            return ctx.parse(NegativeArithmeticExpr.class, cur);
        }

        if (isUnaryOperator(cur.peek())) {
            return ctx.parse(UnaryOperatorExpr.class, cur);
        }

        // (expression)
        var grouped = tryParseGroupedExpression(cur, ctx);
        if (grouped != null) {
            return grouped;
        }

        MatchResult<? extends Expression> matched = ctx.parseIfMatch(CaseExpr.class, cur);
        if (matched.match()) {
            return matched.result();
        }

        matched = ctx.parseIfMatch(CastExpr.class, cur);
        if (matched.match()) {
            return matched.result();
        }

        matched = ctx.parseIfMatch(FunctionExpr.class, cur);
        if (matched.match()) {
            return matched.result();
        }

        matched = ctx.parseIfMatch(AnonymousParamExpr.class, cur);
        if (matched.match()) {
            return matched.result();
        }

        matched = ctx.parseIfMatch(NamedParamExpr.class, cur);
        if (matched.match()) {
            return matched.result();
        }

        matched = ctx.parseIfMatch(OrdinalParamExpr.class, cur);
        if (matched.match()) {
            return matched.result();
        }

        matched = ctx.parseIfMatch(QueryExpr.class, cur);
        if (matched.match()) {
            return matched.result();
        }

        matched = ctx.parseIfMatch(RowExpr.class, cur);
        if (matched.match()) {
            return matched.result();
        }

        matched = ctx.parseIfMatch(ColumnExpr.class, cur);
        if (matched.match()) {
            return matched.result();
        }

        matched = ctx.parseIfMatch(LiteralExpr.class, cur);
        if (matched.match()) {
            return matched.result();
        }

        return error("Unsupported expression token: " + cur.peek().lexeme(), cur.fullPos());
    }

    /**
     * Attempts to parse a parenthesized expression of the form:
     *
     * <pre>
     *     ( expression )
     * </pre>
     *
     * <p>This method performs a look-ahead: if the opening parenthesis is
     * present, the parser commits to parsing a full expression inside, followed
     * by a mandatory closing parenthesis. If an error occurs during the inner
     * parse or the closing parenthesis is missing, the cursor is restored to the
     * state before the {@code '('} and this method returns {@code null},
     * indicating that the parentheses do not introduce an expression.</p>
     *
     * <p>This makes {@code ( expression )} behave as a proper atomic expression
     * while allowing constructs like row constructors {@code (1,2)} to be handled
     * by their own dedicated parsers.</p>
     *
     * @param cur the token cursor positioned at a possible {@code '('}
     * @param ctx the parsing context
     * @return the parsed expression result, or {@code null} if the sequence does
     * not form a parenthesized expression
     */
    private ParseResult<? extends Expression> tryParseGroupedExpression(Cursor cur, ParseContext ctx) {
        if (!cur.match(TokenType.LPAREN)) {
            return null;
        }
        int mark = cur.mark();
        try {
            cur.advance(); // consume '('
            var inner = ctx.parse(Expression.class, cur);
            cur.expect("Expected )", TokenType.RPAREN);
            return inner;
        } catch (ParserException ex) {
            cur.restore(mark);
            return null;
        }
    }

    private static boolean isUnaryOperator(Token t) {
        return OperatorTokens.is(t, "~") || OperatorTokens.is(t, "+");
    }
}
