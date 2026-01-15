package io.sqm.parser;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.ParserException;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.core.OperatorTokens.isComparison;
import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses an <em>atomic predicate</em> — the highest-precedence unit of SQL
 * boolean logic that forms the building blocks beneath {@code AND} and
 * {@code OR} chains.
 *
 * <p>This parser does <strong>not</strong> recognize {@code AND} or {@code OR}
 * operators. Those are handled by {@code AndPredicateParser} and
 * {@code OrPredicateParser}, which sequence together multiple atomic predicates.
 * Instead, this class is responsible for parsing:</p>
 *
 * <ul>
 *   <li>parenthesized predicates: {@code ( ... )}</li>
 *   <li>{@code EXISTS (subquery)}</li>
 *   <li>{@code NOT} predicates (including {@code NOT EXISTS})</li>
 *   <li>all expression-based predicates:
 *       <ul>
 *         <li>comparison: {@code a = b}, {@code a <> b}, ...</li>
 *         <li>{@code BETWEEN} / {@code NOT BETWEEN}</li>
 *         <li>{@code IN} / {@code NOT IN}</li>
 *         <li>{@code LIKE} / {@code NOT LIKE}</li>
 *         <li>{@code IS [NOT] NULL}</li>
 *         <li>unary truth checks (e.g., {@code a IS TRUE})</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <p>This aligns with the standard SQL predicate grammar:</p>
 *
 * <pre>{@code
 * AtomicPredicate
 *     → '(' Predicate ')'
 *     | NOT AtomicPredicate
 *     | EXISTS '(' Query ')'
 *     | Expression ComparisonOp Expression
 *     | Expression {IN|BETWEEN|LIKE|IS ...}
 *     | Expression IS [NOT] {NULL|TRUE|FALSE|IDENT}
 * }</pre>
 *
 * <h2>Grouped predicate disambiguation</h2>
 * <p>
 * When encountering {@code '('}, this parser uses a look-ahead strategy to
 * determine whether the parentheses enclose a <em>full predicate</em> or are
 * part of an <em>expression</em> (e.g. {@code (a) IN (1,2,3)}). If the sequence
 * cannot be parsed as a valid predicate or is followed by tokens that make it
 * impossible for the parentheses to form a predicate, the cursor is restored
 * and the method returns {@code null}, allowing expression-based parsing to
 * continue.
 * </p>
 */
public class AtomicPredicateParser {
    /**
     * Parses the next token sequence as an atomic predicate.
     *
     * <p>This method attempts the following alternatives, in order:</p>
     *
     * <ol>
     *   <li>a parenthesized predicate: {@code ( ... )}</li>
     *   <li>{@code EXISTS (subquery)}</li>
     *   <li>{@code NOT <predicate>} or {@code NOT EXISTS}</li>
     *   <li>expression-based predicates such as:
     *       <ul>
     *         <li>comparisons</li>
     *         <li>{@code BETWEEN} / {@code IN} / {@code LIKE}</li>
     *         <li>{@code IS [NOT] NULL / TRUE / FALSE / IDENT}</li>
     *         <li>unary predicates on simple expressions</li>
     *       </ul>
     *   </li>
     * </ol>
     *
     * <p>If none of these forms match, an error is returned.</p>
     *
     * @param cur the token cursor
     * @param ctx the parsing context
     * @return the parsed predicate or an error result
     */
    public ParseResult<? extends Predicate> parse(Cursor cur, ParseContext ctx) {
        // ( predicate )
        var grouped = tryParseGroupedPredicate(cur, ctx);
        if (grouped != null) {
            return grouped;
        }

        // EXISTS (subquery)
        if (cur.match(TokenType.EXISTS)) {
            return ctx.parse(ExistsPredicate.class, cur);
        }

        // NOT (...) / NOT EXISTS (...)
        if (cur.match(TokenType.NOT)) {
            if (cur.match(TokenType.EXISTS, 1)) {
                return ctx.parse(ExistsPredicate.class, cur);
            }
            return ctx.parse(NotPredicate.class, cur);
        }

        // Expression-based predicates: comparisons, IN, BETWEEN, LIKE, IS, etc.
        return parseExpressionBasedPredicate(cur, ctx);
    }

    /**
     * Attempts to parse a grouped predicate in the form:
     *
     * <pre>{@code
     *     ( predicate )
     * }</pre>
     *
     * <p>The method does the following:</p>
     *
     * <ol>
     *   <li>Checks for an opening parenthesis. If absent, returns {@code null}.</li>
     *   <li>Attempts to parse an inner {@link Predicate} using the full predicate grammar.</li>
     *   <li>Requires a matching {@code ')'}; failure restores the cursor and returns {@code null}.</li>
     *   <li>Performs a look-ahead: if the next token after the closing parenthesis
     *       indicates that the parentheses cannot represent a predicate (e.g.
     *       {@code (a) IN (...)}) then the cursor is restored and the method
     *       returns {@code null}.</li>
     * </ol>
     *
     * <p>This logic allows the parser to correctly distinguish grouped predicates
     * from parenthesized expressions in ambiguous cases.</p>
     *
     * @param cur the token cursor positioned at a potential {@code '('}
     * @param ctx the parsing context
     * @return a parsed grouped predicate, or {@code null} if the input does not form one
     */
    private ParseResult<? extends Predicate> tryParseGroupedPredicate(Cursor cur, ParseContext ctx) {
        if (!cur.match(TokenType.LPAREN)) {
            return null;
        }
        int mark = cur.mark();
        try {
            cur.advance(); // consume '('

            var inner = ctx.parse(Predicate.class, cur);
            cur.expect("Expected )", TokenType.RPAREN);

            if (cur.matchAny(TokenType.AND, TokenType.OR, TokenType.RPAREN, TokenType.EOF)) {
                // Yes, this is a proper grouped predicate
                return inner;
            }

            // e.g. "(a)" in "(a) IN (...)": this is an expression, not a predicate
            cur.restore(mark);
            return null;
        } catch (ParserException ex) {
            // Parsing inner predicate failed → treat as expression
            cur.restore(mark);
            return null;
        }
    }

    /**
     * Parses predicates that begin with an expression, such as:
     *
     * <ul>
     *   <li>comparisons: {@code a = b}, {@code x <= y}, {@code col <> ?}</li>
     *   <li>{@code BETWEEN b AND c}, with optional {@code NOT}</li>
     *   <li>{@code IN (...)}, with optional {@code NOT}</li>
     *   <li>{@code LIKE pattern}, with optional {@code NOT}</li>
     *   <li>{@code IS [NOT] NULL}</li>
     *   <li>{@code IS [NOT] TRUE}, {@code IS [NOT] FALSE}, {@code IS [NOT] ident}</li>
     *   <li>fallback unary predicates for literal or column expressions</li>
     * </ul>
     *
     * <p>The method first parses the left-hand side expression, then inspects the
     * following tokens to determine which predicate form applies.</p>
     *
     * @param cur the token cursor
     * @param ctx the parse context
     * @return the parsed predicate or an error result
     */
    private ParseResult<? extends Predicate> parseExpressionBasedPredicate(Cursor cur, ParseContext ctx) {
        var leftExpr = ctx.parse(Expression.class, cur);

        if (isComparison(cur.peek())) {
            if (cur.match(TokenType.ANY, 1) || cur.match(TokenType.ALL, 1)) {
                return ctx.parse(AnyAllPredicate.class, leftExpr.value(), cur);
            }
            return ctx.parse(ComparisonPredicate.class, leftExpr.value(), cur);
        }

        if (cur.match(TokenType.BETWEEN)) {
            return ctx.parse(BetweenPredicate.class, leftExpr.value(), cur);
        }

        if (cur.match(TokenType.NOT)) {
            if (cur.match(TokenType.BETWEEN, 1)) {
                return ctx.parse(BetweenPredicate.class, leftExpr.value(), cur);
            }

            if (cur.match(TokenType.IN, 1)) {
                return ctx.parse(InPredicate.class, leftExpr.value(), cur);
            }

            if (cur.match(TokenType.LIKE, 1)) {
                return ctx.parse(LikePredicate.class, leftExpr.value(), cur);
            }
        }

        if (cur.match(TokenType.IN)) {
            return ctx.parse(InPredicate.class, leftExpr.value(), cur);
        }

        if (cur.match(TokenType.LIKE)) {
            return ctx.parse(LikePredicate.class, leftExpr.value(), cur);
        }

        if (cur.match(TokenType.IS)) {
            if (cur.match(TokenType.NOT, 1)) {
                if (cur.match(TokenType.NULL, 2)) {
                    return ctx.parse(IsNullPredicate.class, leftExpr.value(), cur);
                }

                if (cur.matchAny(2, TokenType.TRUE, TokenType.FALSE, TokenType.IDENT)) {
                    return ctx.parse(UnaryPredicate.class, leftExpr.value(), cur);
                }
            }

            if (cur.match(TokenType.NULL, 1)) {
                return ctx.parse(IsNullPredicate.class, leftExpr.value(), cur);
            }

            if (cur.matchAny(1, TokenType.TRUE, TokenType.FALSE, TokenType.IDENT)) {
                return ctx.parse(UnaryPredicate.class, leftExpr.value(), cur);
            }
            return error("Expected NULL, TRUE or FALSE after IS", cur.fullPos());
        }

        // 1) Expression-based predicate fallback (dialect-agnostic)
        if (leftExpr.value() instanceof BinaryOperatorExpr) {
            return ok(ExprPredicate.of(leftExpr.value()));
        }

        // 2) Unary predicate fallback (boolean column / literal)
        if (leftExpr.value() instanceof LiteralExpr || leftExpr.value() instanceof ColumnExpr) {
            return ctx.parse(UnaryPredicate.class, leftExpr.value(), cur);
        }

        return error("Expected predicate after expression", cur.fullPos());
    }
}
