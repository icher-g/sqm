package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.InfixParser;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses a relational {@code MATCH_RECOGNIZE (...)} transform.
 */
public class PatternRecognitionTableParser implements MatchableParser<PatternRecognitionTable>,
    InfixParser<TableRef, PatternRecognitionTable> {

    /**
     * Creates a pattern-recognition table parser.
     */
    public PatternRecognitionTableParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends PatternRecognitionTable> parse(Cursor cur, ParseContext ctx) {
        return error("MATCH_RECOGNIZE requires a source table", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<PatternRecognitionTable> parse(TableRef source, Cursor cur, ParseContext ctx) {
        cur.expect("Expected MATCH_RECOGNIZE", TokenType.MATCH_RECOGNIZE);
        if (!ctx.capabilities().supports(SqlFeature.MATCH_RECOGNIZE)) {
            return error("MATCH_RECOGNIZE is not supported by this dialect", cur.fullPos());
        }
        cur.expect("Expected ( after MATCH_RECOGNIZE", TokenType.LPAREN);

        PartitionBy partitionBy = null;
        if (cur.match(TokenType.PARTITION)) {
            var parsed = ctx.parse(PartitionBy.class, cur);
            if (parsed.isError()) return error(parsed);
            partitionBy = parsed.value();
        }

        OrderBy orderBy = null;
        if (cur.match(TokenType.ORDER)) {
            var parsed = ctx.parse(OrderBy.class, cur);
            if (parsed.isError()) return error(parsed);
            orderBy = parsed.value();
        }

        List<PatternMeasure> measures = List.of();
        if (cur.consumeIf(TokenType.MEASURES)) {
            var parsed = parseItems(PatternMeasure.class, cur, ctx);
            if (parsed.isError()) return error(parsed);
            measures = parsed.value();
        }

        RowsPerMatch rowsPerMatch = RowsPerMatch.of(
            RowsPerMatch.Mode.ONE,
            RowsPerMatch.EmptyMatchHandling.DEFAULT
        );
        if (cur.matchAny(TokenType.ONE, TokenType.ALL)) {
            var parsed = ctx.parse(RowsPerMatch.class, cur);
            if (parsed.isError()) return error(parsed);
            rowsPerMatch = parsed.value();
        }

        AfterMatchSkip afterMatchSkip = AfterMatchSkip.of(
            AfterMatchSkip.Kind.PAST_LAST_ROW,
            AfterMatchSkip.Position.DEFAULT,
            null
        );
        if (cur.match(TokenType.AFTER)) {
            var parsed = ctx.parse(AfterMatchSkip.class, cur);
            if (parsed.isError()) return error(parsed);
            afterMatchSkip = parsed.value();
        }

        cur.expect("Expected PATTERN in MATCH_RECOGNIZE", TokenType.PATTERN);
        cur.expect("Expected ( after PATTERN", TokenType.LPAREN);
        var pattern = ctx.parse(MatchPattern.class, cur);
        if (pattern.isError()) return error(pattern);
        cur.expect("Expected ) after PATTERN", TokenType.RPAREN);

        List<PatternSubset> subsets = List.of();
        if (cur.consumeIf(TokenType.SUBSET)) {
            var parsed = parseItems(PatternSubset.class, cur, ctx);
            if (parsed.isError()) return error(parsed);
            subsets = parsed.value();
        }

        cur.expect("Expected DEFINE in MATCH_RECOGNIZE", TokenType.DEFINE);
        var definitions = parseItems(PatternDefinition.class, cur, ctx);
        if (definitions.isError()) return error(definitions);
        cur.expect("Expected ) after MATCH_RECOGNIZE", TokenType.RPAREN);

        return ok(PatternRecognitionTable.of(
            source,
            partitionBy,
            orderBy,
            measures,
            rowsPerMatch,
            afterMatchSkip,
            pattern.value(),
            subsets,
            definitions.value(),
            parseAliasIdentifier(cur)
        ));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PatternRecognitionTable> targetType() {
        return PatternRecognitionTable.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.MATCH_RECOGNIZE);
    }
}
