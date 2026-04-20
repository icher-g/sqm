package io.sqm.playground.rest.service;

import io.sqm.core.DeleteStatement;
import io.sqm.core.InsertStatement;
import io.sqm.core.MergeStatement;
import io.sqm.core.Query;
import io.sqm.core.Statement;
import io.sqm.core.StatementSequence;
import io.sqm.core.UpdateStatement;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.mysql.spi.MySqlSpecs;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseProblem;
import io.sqm.parser.spi.Specs;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import io.sqm.playground.api.DiagnosticPhaseDto;
import io.sqm.playground.api.DiagnosticSeverityDto;
import io.sqm.playground.api.PlaygroundDiagnosticDto;
import io.sqm.playground.api.SqlDialectDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Shared parse and diagnostic helpers for playground statement operations.
 */
@Component
public final class PlaygroundStatementSupport {

    /**
     * Parses SQL text into a statement using the selected dialect.
     *
     * @param sql SQL text
     * @param dialect source dialect
     * @return parse result
     */
    public ParseAttempt parse(String sql, SqlDialectDto dialect) {
        Objects.requireNonNull(sql, "sql must not be null");
        Objects.requireNonNull(dialect, "dialect must not be null");

        var parseContext = ParseContext.of(specsFor(dialect));
        var result = parseContext.parse(Statement.class, sql);
        if (result.isError()) {
            return ParseAttempt.failure(result.problems().stream().map(this::toParseDiagnostic).toList());
        }
        return ParseAttempt.success(result.value());
    }

    /**
     * Parses SQL text into a statement sequence using the selected dialect.
     *
     * @param sql SQL text
     * @param dialect source dialect
     * @return statement sequence parse result
     */
    public SequenceParseAttempt parseSequence(String sql, SqlDialectDto dialect) {
        Objects.requireNonNull(sql, "sql must not be null");
        Objects.requireNonNull(dialect, "dialect must not be null");

        var parseContext = ParseContext.of(specsFor(dialect));
        var result = parseContext.parse(StatementSequence.class, sql);
        if (result.isError()) {
            return SequenceParseAttempt.failure(result.problems().stream().map(this::toParseDiagnostic).toList());
        }
        return SequenceParseAttempt.success(result.value());
    }

    /**
     * Maps a parsed statement to a coarse statement family.
     *
     * @param statement parsed statement
     * @return statement kind
     */
    public String statementKind(Statement statement) {
        Objects.requireNonNull(statement, "statement must not be null");

        return switch (statement) {
            case Query ignored -> "query";
            case InsertStatement ignored -> "insert";
            case UpdateStatement ignored -> "update";
            case DeleteStatement ignored -> "delete";
            case MergeStatement ignored -> "merge";
        };
    }

    /**
     * Creates a structured diagnostic for non-parse operation failures.
     *
     * @param phase operation phase
     * @param code diagnostic code
     * @param message diagnostic message
     * @return structured diagnostic
     */
    public PlaygroundDiagnosticDto diagnostic(DiagnosticPhaseDto phase, String code, String message) {
        return diagnostic(DiagnosticSeverityDto.error, code, message, phase, null, null);
    }

    /**
     * Creates a structured diagnostic for a statement-sequence operation failure.
     *
     * @param phase operation phase
     * @param code diagnostic code
     * @param message diagnostic message
     * @param statementIndex one-based statement index
     * @return structured diagnostic
     */
    public PlaygroundDiagnosticDto diagnostic(DiagnosticPhaseDto phase, String code, String message, int statementIndex) {
        return diagnostic(DiagnosticSeverityDto.error, code, message, phase, null, null, statementIndex);
    }

    /**
     * Creates a structured diagnostic for an operation failure with explicit line and column.
     *
     * @param severity diagnostic severity
     * @param code diagnostic code
     * @param message diagnostic message
     * @param phase operation phase
     * @param line one-based source line when available
     * @param column one-based source column when available
     * @return structured diagnostic
     */
    public PlaygroundDiagnosticDto diagnostic(
        DiagnosticSeverityDto severity,
        String code,
        String message,
        DiagnosticPhaseDto phase,
        Integer line,
        Integer column
    ) {
        return diagnostic(severity, code, message, phase, line, column, null);
    }

    /**
     * Creates a structured diagnostic for an operation failure with explicit location and statement context.
     *
     * @param severity diagnostic severity
     * @param code diagnostic code
     * @param message diagnostic message
     * @param phase operation phase
     * @param line one-based source line when available
     * @param column one-based source column when available
     * @param statementIndex one-based statement index when available
     * @return structured diagnostic
     */
    public PlaygroundDiagnosticDto diagnostic(
        DiagnosticSeverityDto severity,
        String code,
        String message,
        DiagnosticPhaseDto phase,
        Integer line,
        Integer column,
        Integer statementIndex
    ) {
        return new PlaygroundDiagnosticDto(
            severity,
            code,
            message,
            phase,
            line,
            column,
            statementIndex
        );
    }

    private Specs specsFor(SqlDialectDto dialect) {
        return switch (dialect) {
            case ansi -> new AnsiSpecs();
            case postgresql -> new PostgresSpecs();
            case mysql -> new MySqlSpecs();
            case sqlserver -> new SqlServerSpecs();
        };
    }

    private PlaygroundDiagnosticDto toParseDiagnostic(ParseProblem problem) {
        return diagnostic(DiagnosticSeverityDto.error, "PARSE_ERROR", problem.message(), DiagnosticPhaseDto.parse, problem.line(), problem.column());
    }

    /**
     * Parse result wrapper used by playground services.
     *
     * @param statement parsed statement when successful
     * @param diagnostics diagnostics when parsing failed
     */
    public record ParseAttempt(Statement statement, List<PlaygroundDiagnosticDto> diagnostics) {

        /**
         * Creates a successful parse attempt.
         *
         * @param statement parsed statement
         * @return successful parse attempt
         */
        public static ParseAttempt success(Statement statement) {
            return new ParseAttempt(Objects.requireNonNull(statement, "statement must not be null"), List.of());
        }

        /**
         * Creates a failed parse attempt.
         *
         * @param diagnostics parse diagnostics
         * @return failed parse attempt
         */
        public static ParseAttempt failure(List<PlaygroundDiagnosticDto> diagnostics) {
            return new ParseAttempt(null, List.copyOf(diagnostics));
        }

        /**
         * Returns whether parsing succeeded.
         *
         * @return {@code true} when a statement is available
         */
        public boolean success() {
            return statement != null;
        }
    }

    /**
     * Statement sequence parse result wrapper used by playground services.
     *
     * @param sequence parsed statement sequence when successful
     * @param diagnostics diagnostics when parsing failed
     */
    public record SequenceParseAttempt(StatementSequence sequence, List<PlaygroundDiagnosticDto> diagnostics) {

        /**
         * Creates a successful statement sequence parse attempt.
         *
         * @param sequence parsed statement sequence
         * @return successful parse attempt
         */
        public static SequenceParseAttempt success(StatementSequence sequence) {
            return new SequenceParseAttempt(Objects.requireNonNull(sequence, "sequence must not be null"), List.of());
        }

        /**
         * Creates a failed statement sequence parse attempt.
         *
         * @param diagnostics parse diagnostics
         * @return failed parse attempt
         */
        public static SequenceParseAttempt failure(List<PlaygroundDiagnosticDto> diagnostics) {
            return new SequenceParseAttempt(null, List.copyOf(diagnostics));
        }

        /**
         * Returns whether parsing succeeded.
         *
         * @return {@code true} when a statement sequence is available
         */
        public boolean success() {
            return sequence != null;
        }
    }
}
