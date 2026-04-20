package io.sqm.playground.rest.service;

import io.sqm.playground.api.DiagnosticPhaseDto;
import io.sqm.playground.api.PlaygroundDiagnosticDto;
import io.sqm.playground.api.SqlDialectDto;
import io.sqm.playground.api.ValidateRequestDto;
import io.sqm.playground.api.ValidateResponseDto;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.mysql.MySqlValidationDialect;
import io.sqm.validate.postgresql.PostgresValidationDialect;
import io.sqm.validate.schema.SchemaStatementValidator;
import io.sqm.validate.schema.SchemaValidationSettings;
import io.sqm.validate.schema.ValidationCatalogSchemas;
import io.sqm.validate.sqlserver.SqlServerValidationDialect;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Service providing SQL validation responses for the playground.
 */
@Service
public final class ValidateService {

    private final PlaygroundStatementSupport statementSupport;

    /**
     * Creates the validate service.
     *
     * @param statementSupport statement support
     */
    public ValidateService(PlaygroundStatementSupport statementSupport) {
        this.statementSupport = Objects.requireNonNull(statementSupport, "statementSupport must not be null");
    }

    /**
     * Validates SQL against the selected dialect while ignoring schema-specific lookup problems.
     *
     * @param request validate request
     * @return validate response
     */
    public ValidateResponseDto validate(ValidateRequestDto request) {
        Objects.requireNonNull(request, "request must not be null");

        var parseAttempt = statementSupport.parseSequence(request.sql(), request.dialect());
        if (!parseAttempt.success()) {
            return new ValidateResponseDto(
                UUID.randomUUID().toString(),
                false,
                0L,
                false,
                parseAttempt.diagnostics()
            );
        }

        try {
            var validation = validatorFor(request.dialect()).validate(parseAttempt.sequence());
            var diagnostics = validation.problems().stream()
                .map(this::validationDiagnostic)
                .toList();
            if (diagnostics.isEmpty()) {
                return new ValidateResponseDto(
                    UUID.randomUUID().toString(),
                    true,
                    0L,
                    true,
                    List.of()
                );
            }
            return new ValidateResponseDto(
                UUID.randomUUID().toString(),
                true,
                0L,
                false,
                diagnostics
            );
        } catch (RuntimeException e) {
            return new ValidateResponseDto(
                UUID.randomUUID().toString(),
                false,
                0L,
                false,
                List.of(statementSupport.diagnostic(DiagnosticPhaseDto.validate, "VALIDATE_ERROR", e.getMessage()))
            );
        }
    }

    private SchemaStatementValidator validatorFor(SqlDialectDto dialect) {
        return switch (dialect) {
            case ansi -> SchemaStatementValidator.of(ValidationCatalogSchemas.allowEverything(), SchemaValidationSettings.defaults());
            case postgresql -> SchemaStatementValidator.of(ValidationCatalogSchemas.allowEverything(), PostgresValidationDialect.of());
            case mysql -> SchemaStatementValidator.of(ValidationCatalogSchemas.allowEverything(), MySqlValidationDialect.of());
            case sqlserver -> SchemaStatementValidator.of(ValidationCatalogSchemas.allowEverything(), SqlServerValidationDialect.of());
        };
    }

    private PlaygroundDiagnosticDto validationDiagnostic(ValidationProblem problem) {
        return statementSupport.diagnostic(
            DiagnosticPhaseDto.validate,
            problem.code().name(),
            problem.message(),
            problem.statementIndex()
        );
    }
}
