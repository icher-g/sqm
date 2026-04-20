package io.sqm.playground.rest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sqm.core.Node;
import io.sqm.core.Statement;
import io.sqm.core.StatementSequence;
import io.sqm.json.SqmJsonMixins;
import io.sqm.playground.api.ParseRequestDto;
import io.sqm.playground.api.ParseResponseDto;
import io.sqm.playground.api.ParseResponseSummaryDto;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;
import java.util.List;

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
     * @param astMapper        AST mapper
     * @param dslGenerator     DSL generator
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

        var parseAttempt = statementSupport.parseSequence(request.sql(), request.dialect());
        if (!parseAttempt.success()) {
            return new ParseResponseDto(
                UUID.randomUUID().toString(),
                false,
                0L,
                null,
                false,
                null,
                null,
                null,
                null,
                parseAttempt.diagnostics()
            );
        }

        var sequence = parseAttempt.sequence();
        var statements = sequence.statements();
        var root = rootNode(sequence);

        return new ParseResponseDto(
            UUID.randomUUID().toString(),
            true,
            0L,
            statementKind(sequence),
            statements.size() > 1,
            toSqmJson(root),
            toSqmDsl(root, request),
            astMapper.toAst(root),
            summary(root),
            List.of()
        );
    }

    private Node rootNode(StatementSequence sequence) {
        return sequence.statements().size() == 1 ? sequence.statements().getFirst() : sequence;
    }

    private String statementKind(StatementSequence sequence) {
        return sequence.statements().size() == 1 ? statementSupport.statementKind(sequence.statements().getFirst()) : "sequence";
    }

    private String toSqmDsl(Node node, ParseRequestDto request) {
        return switch (node) {
            case Statement statement -> dslGenerator.toDsl(statement, request.dialect());
            case StatementSequence sequence -> dslGenerator.toDsl(sequence, request.dialect());
            default -> throw new IllegalArgumentException("Unsupported parse root node: " + node.getClass().getName());
        };
    }

    private ParseResponseSummaryDto summary(Node node) {
        return new ParseResponseSummaryDto(
            node.getTopLevelInterface().getSimpleName(),
            node.getTopLevelInterface().getName()
        );
    }

    private String toSqmJson(Node node) {
        try {
            return mapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize SQM JSON.", e);
        }
    }
}
