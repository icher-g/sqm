package io.sqm.parser.spi;

import io.sqm.parser.core.ParserException;

import java.util.List;

/**
 * Represents a parsing result.
 *
 * @param value    a parsed value if no error occurred.
 * @param problems a list of errors.
 * @param <T>      the entity type.
 */
public record ParseResult<T>(T value, List<ParseProblem> problems) {
    /**
     * Creates a {@link ParseResult} with the parsed value.
     *
     * @param v   a parsed value.
     * @param <T> a value type.
     * @return {@link ParseResult}.
     */
    public static <T> ParseResult<T> ok(T v) {
        return new ParseResult<>(v, List.of());
    }

    /**
     * Creates a {@link ParseResult} with the error and position where the error occurred.
     *
     * @param message an error.
     * @param pos     a position.
     * @param <T>     a value type.
     * @return {@link ParseResult}.
     */
    public static <T> ParseResult<T> error(String message, int pos) {
        return new ParseResult<>(null, List.of(new ParseProblem(message, pos)));
    }

    /**
     * Creates a {@link ParseResult} with the error and position where the error occurred.
     *
     * @param error an error.
     * @param <T>   a value type.
     * @return {@link ParseResult}.
     */
    public static <T> ParseResult<T> error(ParserException error) {
        return new ParseResult<>(null, List.of(new ParseProblem(error.getMessage(), error.getPos())));
    }

    /**
     * Creates a {@link ParseResult} from another parse result.
     *
     * @param result a parse result to construct the new one from.
     * @param <T>    the entity type.
     * @return {@link ParseResult}.
     */
    public static <T> ParseResult<T> error(ParseResult<?> result) {
        return new ParseResult<>(null, result.problems);
    }

    /**
     * Indicates if the parsing was successful.
     *
     * @return True if the parsing was successful and False otherwise.
     */
    public boolean ok() {
        return problems.isEmpty();
    }

    /**
     * Indicates if the parsing failed.
     *
     * @return True if the parsing failed and False otherwise.
     */
    public boolean isError() {
        return !problems.isEmpty();
    }

    /**
     * Gets a first error message.
     *
     * @return a string representing an error message if there are any.
     */
    public String errorMessage() {
        if (problems.isEmpty()) {
            return null;
        }
        var p = problems.get(0);
        return p.pos() == -1 ? p.message() : p.message() + " at " + p.pos();
    }

    /**
     * Casts a value to a specific type.
     *
     * @param type a type.
     * @param <R>  a return type.
     * @return a value.
     */
    public <R extends T> R valueAs(Class<R> type) {
        return type.cast(value); // safe, throws if mismatched
    }
}
