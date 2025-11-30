package io.sqm.parser.spi;

/**
 * A {@code MatchResult} represents the outcome of a
 * {@code parseIfMatch(...)} invocation on a parser that supports
 * look-ahead matching.
 *
 * <p>The result consists of two pieces of information:
 * <ul>
 *   <li>whether the parser <em>matched</em> the current input position,
 *       i.e. whether it recognizes the upcoming token sequence as a construct
 *       it is capable of parsing; and</li>
 *   <li>the {@link ParseResult} produced by the parser if it matched.</li>
 * </ul>
 *
 * <p>This structure allows callers (typically {@link ParseContext} or a
 * higher-level parser such as {@code PrimaryExpressionParser}) to distinguish
 * between:
 *
 * <ul>
 *   <li><strong>No match</strong> –
 *       the parser is not applicable at the current cursor position,
 *       and therefore should neither consume input nor signal an error.</li>
 *
 *   <li><strong>Successful match</strong> –
 *       the parser recognized the upcoming construct and successfully parsed it,
 *       returning a completed {@code ParseResult}.</li>
 *
 *   <li><strong>Failed match</strong> –
 *       the parser recognized the construct (i.e. matching succeeded),
 *       but the actual parse failed and returned a
 *       {@code ParseResult.failure(...)}, carrying an error message.</li>
 * </ul>
 *
 * <p>Unlike {@link ParseResult}, which represents the outcome of an actual
 * parsing attempt, {@code MatchResult} captures the outcome of a combined
 * look-ahead + parse operation used by the framework to resolve ambiguity
 * between multiple parsers.
 *
 * <p>The static constructors {@link #matched(ParseResult)} and
 * {@link #notMatched()} provide convenient creation of the two primary
 * outcomes.
 *
 * @param <T> the type of AST node produced by the associated parser
 */
public record MatchResult<T>(boolean match, ParseResult<T> result) {

    /**
     * Creates a {@code MatchResult} indicating that the parser matched
     * the current input and that the provided {@link ParseResult} should
     * be returned as the parsing outcome.
     *
     * <p>If the wrapped {@code ParseResult} represents a failure, this
     * indicates a <em>matched but unsuccessful</em> parse. If it represents
     * a success, then the entire {@code parseIfMatch} operation succeeded.
     *
     * @param result the parsing result produced by the matching parser
     * @param <T>    the node type contained in the parsing result
     * @return a {@code MatchResult} reflecting a matched parser
     */
    public static <T> MatchResult<T> matched(ParseResult<T> result) {
        return new MatchResult<>(true, result);
    }

    /**
     * Creates a {@code MatchResult} indicating that the parser does not
     * match the input at the current cursor position.
     *
     * <p>This signals that the parser should be skipped and that another
     * parser in a chain may attempt to match instead. A non-matching parser
     * must not consume any input.
     *
     * @param <T> the node type associated with the parser
     * @return a {@code MatchResult} representing a non-match
     */
    public static <T> MatchResult<T> notMatched() {
        return new MatchResult<>(false, null);
    }
}

