package io.sqm.playground.rest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sqm.core.Statement;
import io.sqm.json.SqmJsonMixins;
import io.sqm.playground.api.ParseRequestDto;
import io.sqm.playground.api.ParseResponseDto;
import io.sqm.playground.api.ParseResponseSummaryDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Service providing SQL parse responses for the playground.
 */
@Service
public final class ParseService {

    private final ObjectMapper mapper;
    private final SqmAstMapper astMapper;
    private final SqmDslGenerator dslGenerator;
    private final PlaygroundStatementSupport statementSupport;

    /**
     * Creates the parse service.
     *
     * @param astMapper AST mapper
     * @param dslGenerator DSL generator
     * @param statementSupport statement support
     */
    public ParseService(SqmAstMapper astMapper, SqmDslGenerator dslGenerator, PlaygroundStatementSupport statementSupport) {
        this.mapper = SqmJsonMixins.createPretty();
        this.astMapper = Objects.requireNonNull(astMapper, "astMapper must not be null");
        this.dslGenerator = Objects.requireNonNull(dslGenerator, "dslGenerator must not be null");
        this.statementSupport = Objects.requireNonNull(statementSupport, "statementSupport must not be null");
    }

    /**
     * Parses SQL text using the requested dialect.
     *
     * @param request parse request
     * @return parse response
     */
    public ParseResponseDto parse(ParseRequestDto request) {
        Objects.requireNonNull(request, "request must not be null");

        var parseAttempt = statementSupport.parse(request.sql(), request.dialect());
        if (!parseAttempt.success()) {
            return new ParseResponseDto(
                UUID.randomUUID().toString(),
                false,
                0L,
                null,
                null,
                null,
                null,
                null,
                parseAttempt.diagnostics()
            );
        }

        var statement = parseAttempt.statement();
        return new ParseResponseDto(
            UUID.randomUUID().toString(),
            true,
            0L,
            statementSupport.statementKind(statement),
            toSqmJson(statement),
            dslGenerator.toDsl(statement, request.dialect()),
            astMapper.toAst(statement),
            new ParseResponseSummaryDto(
                statement.getClass().getSimpleName(),
                statement.getTopLevelInterface().getName()
            ),
            List.of()
        );
    }

    private String toSqmJson(Statement statement) {
        try {
            return mapper.writeValueAsString(statement);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize SQM JSON.", e);
        }
    }
}
