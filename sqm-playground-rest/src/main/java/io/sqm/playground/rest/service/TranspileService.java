package io.sqm.playground.rest.service;

import io.sqm.playground.api.DiagnosticPhaseDto;
import io.sqm.playground.api.DiagnosticSeverityDto;
import io.sqm.playground.api.PlaygroundDiagnosticDto;
import io.sqm.playground.api.TranspileOutcomeDto;
import io.sqm.playground.api.TranspileRequestDto;
import io.sqm.playground.api.TranspileResponseDto;
import io.sqm.transpile.SqlTranspiler;
import io.sqm.transpile.TranspileOptions;
import io.sqm.transpile.TranspileResult;
import io.sqm.transpile.TranspileStatus;
import io.sqm.validate.schema.ValidationCatalogSchemas;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Service providing SQL transpilation responses for the playground.
 */
@Service
public final class TranspileService {

    /**
     * Transpiles SQL from the selected source dialect into the selected target dialect.
     *
     * @param request transpile request
     * @return transpile response
     */
    public TranspileResponseDto transpile(TranspileRequestDto request) {
        Objects.requireNonNull(request, "request must not be null");

        try {
            var result = SqlTranspiler.builder()
                .sourceDialect(PlaygroundDialectSupport.toDialectId(request.sourceDialect()))
                .targetDialect(PlaygroundDialectSupport.toDialectId(request.targetDialect()))
                .sourceSchema(ValidationCatalogSchemas.allowEverything())
                .targetSchema(ValidationCatalogSchemas.allowEverything())
                .options(new TranspileOptions(true, false, true, true))
                .build()
                .transpile(request.sql());

            return new TranspileResponseDto(
                UUID.randomUUID().toString(),
                result.success(),
                0L,
                toOutcome(result),
                result.sql().orElse(null),
                toDiagnostics(result)
            );
        } catch (RuntimeException e) {
            return new TranspileResponseDto(
                UUID.randomUUID().toString(),
                false,
                0L,
                TranspileOutcomeDto.unsupported,
                null,
                List.of(new PlaygroundDiagnosticDto(
                    DiagnosticSeverityDto.error,
                    "TRANSPILE_ERROR",
                    e.getMessage(),
                    DiagnosticPhaseDto.transpile,
                    null,
                    null
                ))
            );
        }
    }

    private static TranspileOutcomeDto toOutcome(TranspileResult result) {
        if (result.status() == TranspileStatus.SUCCESS) {
            return TranspileOutcomeDto.exact;
        }
        if (result.status() == TranspileStatus.SUCCESS_WITH_WARNINGS) {
            return TranspileOutcomeDto.approximate;
        }
        return TranspileOutcomeDto.unsupported;
    }

    private static List<PlaygroundDiagnosticDto> toDiagnostics(TranspileResult result) {
        var diagnostics = new ArrayList<PlaygroundDiagnosticDto>(result.problems().size() + result.warnings().size());
        for (var problem : result.problems()) {
            diagnostics.add(new PlaygroundDiagnosticDto(
                DiagnosticSeverityDto.error,
                problem.code(),
                problem.message(),
                DiagnosticPhaseDto.transpile,
                null,
                null
            ));
        }
        for (var warning : result.warnings()) {
            diagnostics.add(new PlaygroundDiagnosticDto(
                DiagnosticSeverityDto.warning,
                warning.code(),
                warning.message(),
                DiagnosticPhaseDto.transpile,
                null,
                null
            ));
        }
        return List.copyOf(diagnostics);
    }
}
