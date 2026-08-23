package io.sqm.parser.ansi;

import io.sqm.core.MatchPattern;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Shared recursive-descent grammar for the contents of a {@code PATTERN (...)} clause.
 * <p>
 * The methods model the pattern precedence levels from lowest to highest:
 * alternation, sequence, quantification, and primary. Grouping is consumed while
 * parsing and is therefore represented by the resulting tree shape rather than by
 * a dedicated semantic node.
 */
final class PatternGrammar {
    private PatternGrammar() {
    }

    /**
     * Parses one or more sequences separated by {@code |}.
     * <p>Examples: {@code A | B}, {@code A B+ | C D}.</p>
     *
     * @param cur cursor positioned at the first alternative
     * @param ctx parser context
     * @return parsed alternation, or its sole sequence when no {@code |} is present
     */
    static ParseResult<? extends MatchPattern> parseAlternation(Cursor cur, ParseContext ctx) {
        var first = parseSequence(cur, ctx);
        if (first.isError()) return error(first);
        List<MatchPattern> alternatives = new ArrayList<>();
        alternatives.add(first.value());
        while (isOperator(cur, "|")) {
            cur.advance();
            var next = parseSequence(cur, ctx);
            if (next.isError()) return error(next);
            alternatives.add(next.value());
        }
        return alternatives.size() == 1 ? first : ok(MatchPattern.Alternation.of(alternatives));
    }

    /**
     * Parses adjacent quantified or primary patterns as a sequence.
     * <p>Examples: {@code A B}, {@code A+ PERMUTE(B, C)}, {@code ^ A $}.</p>
     * Parsing stops before an alternation separator, comma, or closing parenthesis.
     *
     * @param cur cursor positioned at the first sequence element
     * @param ctx parser context
     * @return parsed sequence, or its sole element
     */
    static ParseResult<? extends MatchPattern> parseSequence(Cursor cur, ParseContext ctx) {
        List<MatchPattern> elements = new ArrayList<>();
        while (startsPrimary(cur)) {
            var element = parseQuantified(cur, ctx);
            if (element.isError()) return error(element);
            elements.add(element.value());
        }
        if (elements.isEmpty()) return error("Expected row pattern", cur.fullPos());
        return elements.size() == 1 ? ok(elements.getFirst()) : ok(MatchPattern.Sequence.of(elements));
    }

    /**
     * Parses a primary followed by an optional greedy or reluctant quantifier.
     * <p>Examples: {@code A*}, {@code A+?}, <code>(A B){2,5}?</code>, and <code>A{,3}</code>.</p>
     *
     * @param cur cursor positioned at the quantified primary
     * @param ctx parser context
     * @return parsed quantified pattern, or the unquantified primary
     */
    static ParseResult<? extends MatchPattern> parseQuantified(Cursor cur, ParseContext ctx) {
        var primary = parsePrimary(cur, ctx);
        if (primary.isError()) return error(primary);
        Integer minimum;
        Integer maximum;
        if (isOperator(cur, "*")) {
            cur.advance();
            minimum = 0;
            maximum = null;
        }
        else {
            if (isOperator(cur, "+")) {
                cur.advance();
                minimum = 1;
                maximum = null;
            }
            else {
                if (cur.consumeIf(TokenType.QMARK)) {
                    minimum = 0;
                    maximum = 1;
                }
                else {
                    if (cur.match(TokenType.LBRACE) && !isOperator(cur, "-", 1)) {
                        cur.advance();
                        if (cur.consumeIf(TokenType.COMMA)) {
                            minimum = 0;
                            maximum = parseBound(cur, "Expected upper quantifier bound");
                        }
                        else {
                            minimum = parseBound(cur, "Expected quantifier bound");
                            if (cur.consumeIf(TokenType.COMMA)) {
                                maximum = cur.match(TokenType.NUMBER)
                                    ? parseBound(cur, "Expected upper quantifier bound") : null;
                            }
                            else maximum = minimum;
                        }
                        cur.expect("Expected } after quantifier", TokenType.RBRACE);
                    }
                    else {
                        return primary;
                    }
                }
            }
        }
        boolean reluctant = cur.consumeIf(TokenType.QMARK);
        return ok(MatchPattern.Quantified.of(primary.value(), minimum, maximum, reluctant));
    }

    /**
     * Parses one pattern primary.
     * <p>Examples: {@code A}, {@code (A B)}, {@code PERMUTE(A, B)}, {@code ^},
     * {@code $}, {@code ()}, and <code>{- A B -}</code>.</p>
     *
     * @param cur cursor positioned at the primary
     * @param ctx parser context
     * @return parsed primary
     */
    static ParseResult<? extends MatchPattern> parsePrimary(Cursor cur, ParseContext ctx) {
        if (cur.match(TokenType.IDENT)) return ctx.parse(MatchPattern.Variable.class, cur);
        if (cur.match(TokenType.PERMUTE)) return ctx.parse(MatchPattern.Permutation.class, cur);
        if (isOperator(cur, "^") || cur.match(TokenType.DOLLAR)) return ctx.parse(MatchPattern.Anchor.class, cur);
        if (cur.match(TokenType.LPAREN) && cur.match(TokenType.RPAREN, 1)) return ctx.parse(MatchPattern.Empty.class, cur);
        if (cur.match(TokenType.LBRACE) && isOperator(cur, "-", 1)) return ctx.parse(MatchPattern.Exclusion.class, cur);
        if (cur.consumeIf(TokenType.LPAREN)) {
            var grouped = ctx.parse(MatchPattern.class, cur);
            cur.expect("Expected ) after grouped row pattern", TokenType.RPAREN);
            return grouped;
        }
        return error("Expected row-pattern primary", cur.fullPos());
    }

    /**
     * Determines whether the cursor starts a pattern primary such as {@code A},
     * {@code (A B)}, {@code PERMUTE(A, B)}, {@code ^}, {@code $}, or <code>{- A -}</code>.
     *
     * @param cur cursor to inspect
     * @return {@code true} when a primary can start at the current token
     */
    static boolean startsPrimary(Cursor cur) {
        return cur.match(TokenType.IDENT)
            || cur.match(TokenType.PERMUTE)
            || cur.match(TokenType.LPAREN)
            || cur.match(TokenType.LBRACE)
            || cur.match(TokenType.DOLLAR)
            || isOperator(cur, "^");
    }

    /**
     * Tests the current token for an operator lexeme such as {@code |}, {@code *},
     * {@code +}, {@code -}, or {@code ^}.
     *
     * @param cur    cursor to inspect
     * @param lexeme expected operator text
     * @return {@code true} when the current token is the requested operator
     */
    static boolean isOperator(Cursor cur, String lexeme) {
        return isOperator(cur, lexeme, 0);
    }

    /**
     * Tests a lookahead token for an operator lexeme; for example, offset {@code 1}
     * recognizes the {@code -} in the exclusion opener {@code {-}.
     *
     * @param cur    cursor to inspect
     * @param lexeme expected operator text
     * @param offset zero-based token offset from the current position
     * @return {@code true} when the lookahead token is the requested operator
     */
    static boolean isOperator(Cursor cur, String lexeme, int offset) {
        return cur.match(TokenType.OPERATOR, lexeme, offset);
    }

    /**
     * Parses one integer quantifier bound.
     * <p>Examples: the {@code 2} in <code>A{2}</code> and the {@code 5} in <code>A{2,5}</code>.</p>
     *
     * @param cur     cursor positioned at the numeric bound
     * @param message diagnostic used when the bound is absent or invalid
     * @return parsed integer bound
     */
    static Integer parseBound(Cursor cur, String message) {
        var token = cur.expect(message, TokenType.NUMBER);
        try {
            return Integer.valueOf(token.lexeme());
        } catch (NumberFormatException ex) {
            throw new io.sqm.parser.core.ParserException(message, token.pos());
        }
    }
}
