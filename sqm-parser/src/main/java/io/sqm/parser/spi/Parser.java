package io.sqm.parser.spi;

import io.sqm.core.Entity;
import io.sqm.core.repos.Handler;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.Lexer;
import io.sqm.parser.core.ParserException;
import io.sqm.parser.core.TokenType;

import java.util.Objects;

/**
 * A base interface for all spec parsers.
 *
 * @param <T> the type of the entity.
 */
public interface Parser<T extends Entity> extends Handler<T> {

    /**
     * A default implementation of the parse method that accepts a spec as a string.
     * The method converts the spec into a {@link Cursor} and calls {@link Parser#parse(Cursor, ParseContext)} method.
     *
     * @param spec a spec string
     * @return a parsing result.
     */
    default ParseResult<T> parse(String spec, ParseContext ctx) {
        Objects.requireNonNull(spec, "spec cannot be null.");

        if (spec.isBlank()) {
            return ParseResult.error("The spec cannot be blank.", -1);
        }

        try {
            var ts = Lexer.lexAll(spec);
            return parse(new Cursor(ts), ctx);
        } catch (ParserException ex) {
            return ParseResult.error(ex);
        }
    }

    /**
     * Parses a string into a number. Double or Long.
     *
     * @param lexeme a string to parse.
     * @return If the parsing is successful then double or long is returned; if not then the original string is returned.
     */
    default Object parseNumber(String lexeme) {
        try {
            if (lexeme.contains(".") || lexeme.contains("e") || lexeme.contains("E")) {
                return Double.valueOf(lexeme);
            }
            return Long.valueOf(lexeme);
        } catch (NumberFormatException nfe) {
            // fallback to string if something exotic slips through
            return lexeme;
        }
    }

    /**
     * Parses an alias if available.
     *
     * @param cur a cursor containing tokens to parse.
     * @return an alis if exists or null otherwise.
     */
    default String parseAlias(Cursor cur) {
        if (cur.consumeIf(TokenType.AS)) {
            return cur.expect("Expected alias after AS", TokenType.IDENT).lexeme();
        }
        if (cur.match(TokenType.IDENT)) {
            return cur.advance().lexeme();
        }
        // no alias
        return null;
    }

    /**
     * Finalizes the parsing by applying some validations. Checks if the provided {@link ParseResult} is valid and if the cursor is on the EOF token.
     * If everything ok the valid {@link ParseResult} instance.
     *
     * @param cur a cursor.
     * @param ctx a parsing context.
     * @param pr  a parsing result.
     * @return a valid or invalid parsing result depending on the validation result.
     */
    default ParseResult<T> finalize(Cursor cur, ParseContext ctx, ParseResult<? extends T> pr) {
        if (pr.isError()) {
            return error(pr);
        }
        if (ctx.callstack() == 0 && !cur.isEof()) {
            return error("Expected EOF but found: " + cur.peek().lexeme(), cur.fullPos());
        }
        return ok(pr.value());
    }

    /**
     * Creates a valid {@link ParseResult}.
     *
     * @param v   a type of the value.
     * @param <R> a value type.
     * @return a valid {@link ParseResult}.
     */
    default <R> ParseResult<R> ok(R v) {
        return ParseResult.ok(v);
    }

    /**
     * Creates invalid {@link ParseResult}.
     *
     * @param message an error message to provide with the invalid result.
     * @param pos     a position where the error occurred.
     * @param <R>     a value type.
     * @return an invalid {@link ParseResult}.
     */
    default <R> ParseResult<R> error(String message, int pos) {
        return ParseResult.error(message, pos);
    }

    /**
     * Creates invalid {@link ParseResult}.
     *
     * @param error an exception to be used to create invalid parsing result.
     * @param <R>   a result type.
     * @return an invalid {@link ParseResult}.
     */
    default <R> ParseResult<R> error(ParserException error) {
        return ParseResult.error(error);
    }

    /**
     * Creates invalid {@link ParseResult} from another {@link ParseResult} by coping the errors.
     *
     * @param result a {@link ParseResult} to copy the errors from.
     * @param <R>    a result type.
     * @return an invalid {@link ParseResult}.
     */
    default <R> ParseResult<R> error(ParseResult<?> result) {
        return ParseResult.error(result);
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    ParseResult<T> parse(Cursor cur, ParseContext ctx);
}
