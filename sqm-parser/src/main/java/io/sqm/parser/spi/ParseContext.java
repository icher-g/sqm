package io.sqm.parser.spi;

import io.sqm.core.Node;
import io.sqm.parser.DefaultParseContext;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.ParserException;
import io.sqm.parser.core.TokenType;

import java.util.Deque;
import java.util.Objects;

import static io.sqm.parser.spi.ParseResult.error;

/**
 * A parsing context provided to {@link Parser} instances during parsing.
 * <p>
 * The {@code ParseContext} coordinates parser lookup, manages grammar-aware
 * features such as lookups and infix parsing, and maintains a logical parse
 * call stack that reflects the active parsing frames. This separates parsing
 * flow control from the JVM call stack and allows grammar rules to make
 * context-dependent decisions safely.
 */
public interface ParseContext {

    /**
     * Creates a new {@link ParseContext} backed by the provided {@link Specs}
     * implementation.
     *
     * @param specs dialect-aware parsing specifications
     * @return a new {@link ParseContext} instance
     */
    static ParseContext of(Specs specs) {
        return new DefaultParseContext(specs);
    }

    /**
     * Returns the repository of all available parsers.
     *
     * @return the parser repository
     */
    ParsersRepository parsers();

    /**
     * Returns the {@link Lookups} implementation used for grammar-specific
     * look-ahead and other structural decisions.
     *
     * @return the lookups implementation
     */
    Lookups lookups();

    /**
     * Returns the logical parse call stack maintained by this context.
     * <p>
     * Unlike the JVM call stack, this stack contains the sequence of AST types
     * corresponding to active {@code ctx.parse(Type.class, ...)} calls.
     * Each pushed class represents an active parsing frame for a specific
     * {@link Node} subtype.
     * <p>
     * The stack allows complex grammar rules—such as distinguishing between
     * top-level and nested predicate parsing—to make informed decisions without
     * depending on Java call-stack inspection.
     *
     * @return a {@link Deque} whose top element represents the most recent
     * parsing frame
     */
    Deque<Class<?>> callstack();

    /**
     * Parses a textual specification into a node of the given type.
     *
     * @param type the expected result type
     * @param spec the textual representation to parse
     * @param <T>  node type
     * @return a {@link ParseResult} containing either the parsed node or an error
     */
    default <T extends Node> ParseResult<? extends T> parse(Class<T> type, String spec) {
        Objects.requireNonNull(spec, "spec cannot be null.");
        if (spec.isBlank()) {
            return error("The spec cannot be blank.", -1);
        }
        Parser<T> parser;
        Cursor cur;
        try {
            parser = parsers().require(type);
            cur = Cursor.of(spec);
        } catch (Exception e) {
            return error(e.getMessage(), -1);
        }
        return parse(parser, cur);
    }

    /**
     * Low-level entry point for parsing using a resolved {@link Parser}.
     * Handles call-stack management and finalization.
     */
    default <T extends Node> ParseResult<? extends T> parse(Parser<T> parser, String spec) {
        Objects.requireNonNull(spec, "spec cannot be null.");
        if (spec.isBlank()) {
            return error("The spec cannot be blank.", -1);
        }
        Cursor cur;
        try {
            cur = Cursor.of(spec);
        } catch (Exception e) {
            return error(e.getMessage(), -1);
        }
        return parse(parser, cur);
    }

    /**
     * Parses a node of the given type using an existing {@link Cursor}.
     *
     * @param type the expected result type
     * @param cur  a cursor positioned at the start of the structure to parse
     * @param <T>  node type
     * @return a {@link ParseResult} containing the parsed node or an error
     */
    default <T extends Node> ParseResult<? extends T> parse(Class<T> type, Cursor cur) {
        Parser<T> parser;
        try {
            parser = parsers().require(type);
        } catch (Exception e) {
            return error(e.getMessage(), -1);
        }
        return parse(parser, cur);
    }


    /**
     * Low-level entry point for parsing using a resolved {@link Parser}.
     * Handles call-stack management and finalization.
     */
    default <T extends Node> ParseResult<? extends T> parse(Parser<T> parser, Cursor cur) {
        ParseResult<? extends T> result;
        callstack().push(parser.targetType());
        try {
            result = parser.parse(cur, this);
        } catch (ParserException e) {
            return error(e);
        } catch (Exception e) {
            return error(e.getMessage(), -1);
        } finally {
            callstack().pop();
        }
        return finalize(cur, result);
    }

    /**
     * Parses an infix construct where the left-hand operand has already been parsed.
     *
     * @param type the expected result type
     * @param lhs  the already parsed left-hand operand
     * @param cur  the current cursor
     * @param <O>  the type of the left-hand operand
     * @param <T>  the target node type
     * @return a parsing result for the combined expression
     */
    @SuppressWarnings("unchecked")
    default <O extends Node, T extends Node> ParseResult<? extends T> parse(Class<T> type, O lhs, Cursor cur) {
        ParseResult<? extends T> result;
        callstack().push(type);
        try {
            var parser = parsers().require(type);
            if (parser instanceof InfixParser<?, ?> infixParser) {
                var infix = (InfixParser<O, T>) infixParser;
                result = infix.parse(lhs, cur, this);
            }
            else {
                return error("Parser " + type.getSimpleName() + " does not support infix parse(lhs, ...)", 0);
            }
        } catch (ParserException e) {
            return error(e);
        } catch (Exception e) {
            return error(e.getMessage(), -1);
        } finally {
            callstack().pop();
        }
        return finalize(cur, result);
    }

    /**
     * Attempts to parse a value of the given type, optionally wrapped in parentheses.
     *
     * @param type the expected result type
     * @param cur  the current cursor
     * @param <T>  node type
     * @return a parsing result
     */
    default <T extends Node> ParseResult<? extends T> parseEnclosed(Class<T> type, Cursor cur) {
        if (cur.consumeIf(TokenType.LPAREN)) {
            var result = parse(type, cur);
            cur.expect("Expected )", TokenType.RPAREN);
            return result;
        }
        return parse(type, cur);
    }

    /**
     * Performs match-then-parse logic for a {@link MatchableParser}.
     * <p>
     * Workflow:
     * <ol>
     *     <li>Call {@code parser.match(cur, this)} without consuming input.</li>
     *     <li>If the parser does not match, return {@link MatchResult#notMatched()}.</li>
     *     <li>If it matches, consume input via {@code parser.parse(...)}
     *         and wrap the result in a {@link MatchResult#matched(ParseResult)}.</li>
     * </ol>
     *
     * @param type the node type to attempt matching
     * @param cur  the current cursor
     * @param <T>  node type
     * @return a {@link MatchResult} describing whether the parser matched
     */
    default <T extends Node> MatchResult<? extends T> parseIfMatch(Class<T> type, Cursor cur) {
        ParseResult<? extends T> result = null;
        callstack().push(type);
        try {
            var parser = parsers().require(type);
            if (parser instanceof MatchableParser<T> matchableParser) {
                if (matchableParser.match(cur, this)) {
                    result = matchableParser.parse(cur, this);
                }
            }
            else {
                return MatchResult.matched(error("Parser " + type.getSimpleName() + " does not support match(Cursor, ParseContext)", 0));
            }
        } catch (ParserException e) {
            return MatchResult.matched(error(e));
        } catch (Exception e) {
            return MatchResult.matched(error(e.getMessage(), -1));
        } finally {
            callstack().pop();
        }
        return (result == null)
            ? MatchResult.notMatched()
            : MatchResult.matched(finalize(cur, result));
    }

    /**
     * Finalizes parsing by validating the result and ensuring no trailing tokens
     * remain when at the top level.
     *
     * @param cur a cursor
     * @param pr  a parsing result
     * @return the finalized parsing result
     */
    default <T> ParseResult<? extends T> finalize(Cursor cur, ParseResult<? extends T> pr) {
        if (pr.isError()) return pr;
        if (callstack().isEmpty() && !cur.isEof()) {
            return error("Expected EOF but found: " + cur.peek().lexeme(), cur.fullPos());
        }
        return pr;
    }
}

