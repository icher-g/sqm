package io.cherlabs.sqlmodel.parser;

import io.cherlabs.sqlmodel.core.Entity;

import java.util.List;

public record ParseResult<T>(T value, List<ParseProblem> problems) {
    public static <T> ParseResult<T> ok(T v) {
        return new ParseResult<>(v, List.of());
    }

    public static <T> ParseResult<T> error(String message, int pos) {
        return new ParseResult<>(null, List.of(new ParseProblem(message, pos)));
    }

    public static <T> ParseResult<T> error(String message) {
        return new ParseResult<>(null, List.of(new ParseProblem(message, -1)));
    }

    public static <T> ParseResult<T> error(Exception error, int pos) {
        return new ParseResult<>(null, List.of(new ParseProblem(error.getMessage(), pos)));
    }

    public static <T> ParseResult<T> error(Exception error) {
        return new ParseResult<>(null, List.of(new ParseProblem(error.getMessage(), -1)));
    }

    public static <T> ParseResult<T> error(ParseResult<? extends Entity> result) {
        var problem = result.problems.get(0);
        return new ParseResult<>(null, List.of(problem));
    }

    public boolean ok() {
        return problems.isEmpty();
    }

    public String errorMessage() {
        return problems.isEmpty() ? null : problems.get(0).message();
    }

    public <R extends T> R valueAs(Class<R> type) {
        return type.cast(value); // safe, throws if mismatched
    }
}
