package io.sqm.parser.spi;

import io.sqm.core.Node;
import io.sqm.parser.core.Cursor;

/**
 * A {@code MatchableParser} is a specialized {@link Parser} for constructs
 * that can participate in disambiguation via a preliminary <em>match</em>
 * step.
 * <p>
 * Typical usage is:
 * <ol>
 *   <li>Call {@link #match(Cursor, ParseContext)} as a pure look-ahead
 *       operation to decide whether this parser is applicable at the
 *       current cursor position.</li>
 *   <li>If it returns {@code true}, call
 *       {@link #parse(Cursor, ParseContext)} to actually consume tokens
 *       and produce a {@link ParseResult}.</li>
 * </ol>
 * <p>
 * This pattern is commonly wrapped by
 * {@link ParseContext#parseIfMatch(Class, Cursor)}, which combines
 * both steps and returns a {@link MatchResult} describing whether the parser
 * matched and, if so, what the parse result was.
 *
 * <p>Contract:</p>
 * <ul>
 *   <li>{@link #match(Cursor, ParseContext)} must be a <strong>look-ahead only</strong>
 *       operation: it must not advance the cursor or mutate the parse context.</li>
 *   <li>{@link #parse(Cursor, ParseContext)} is only invoked if
 *       {@code match(...)} has just returned {@code true} and may assume
 *       that the input indeed belongs to this grammar construct.</li>
 *   <li>If parsing fails despite a successful match, {@code parse(...)}
 *       must return a failure {@link ParseResult} describing the error.</li>
 * </ul>
 *
 * @param <T> the specific {@link Node} type produced by this parser
 */
public interface MatchableParser<T extends Node> extends Parser<T> {

    /**
     * Performs a look-ahead test to determine whether this parser is applicable
     * at the current cursor position.
     * <p>
     * Implementations must <strong>not</strong> advance the cursor or modify
     * the {@link ParseContext}. Their sole responsibility is to inspect the
     * upcoming tokens and decide if this parser is responsible for them.
     *
     * @param cur the cursor pointing at the current token
     * @param ctx the parsing context providing configuration and utilities
     * @return {@code true} if this parser should be used to parse the upcoming
     *         input, {@code false} otherwise
     */
    boolean match(Cursor cur, ParseContext ctx);
}


