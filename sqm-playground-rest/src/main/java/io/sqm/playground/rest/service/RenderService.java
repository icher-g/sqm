package io.sqm.playground.rest.service;

import io.sqm.control.execution.ExecutionContext;
import io.sqm.control.execution.ExecutionMode;
import io.sqm.control.execution.ParameterizationMode;
import io.sqm.control.pipeline.SqlStatementRenderer;
import io.sqm.playground.api.DiagnosticPhaseDto;
import io.sqm.playground.api.RenderParameterizationModeDto;
import io.sqm.playground.api.RenderRequestDto;
import io.sqm.playground.api.RenderResponseDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Service providing SQL rendering responses for the playground.
 */
@Service
public final class RenderService {

    private final PlaygroundStatementSupport statementSupport;
    private final SqlStatementRenderer renderer;

    /**
     * Creates the render service.
     *
     * @param statementSupport statement support
     */
    public RenderService(PlaygroundStatementSupport statementSupport) {
        this.statementSupport = Objects.requireNonNull(statementSupport, "statementSupport must not be null");
        this.renderer = SqlStatementRenderer.standard();
    }

    /**
     * Renders SQL for the requested target dialect.
     *
     * @param request render request
     * @return render response
     */
    public RenderResponseDto render(RenderRequestDto request) {
        Objects.requireNonNull(request, "request must not be null");

        var parseAttempt = statementSupport.parseSequence(request.sql(), request.sourceDialect());
        if (!parseAttempt.success()) {
            return new RenderResponseDto(
                UUID.randomUUID().toString(),
                false,
                0L,
                null,
                List.of(),
                parseAttempt.diagnostics()
            );
        }

        try {
            var context = ExecutionContext.of(
                PlaygroundDialectSupport.toDialectId(request.targetDialect()),
                null,
                null,
                ExecutionMode.ANALYZE,
                toParameterizationMode(request.parameterizationMode())
            );
            var rendered = renderer.render(parseAttempt.sequence(), context);
            return new RenderResponseDto(
                UUID.randomUUID().toString(),
                true,
                0L,
                rendered.sql(),
                rendered.params(),
                List.of()
            );
        } catch (RuntimeException e) {
            return new RenderResponseDto(
                UUID.randomUUID().toString(),
                false,
                0L,
                null,
                List.of(),
                List.of(statementSupport.diagnostic(DiagnosticPhaseDto.render, "RENDER_ERROR", e.getMessage()))
            );
        }
    }

    private static ParameterizationMode toParameterizationMode(RenderParameterizationModeDto mode) {
        return switch (mode) {
            case inline -> ParameterizationMode.OFF;
            case bind -> ParameterizationMode.BIND;
        };
    }
}
