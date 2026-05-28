package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.SampledTable;
import io.sqm.core.TableRef;
import io.sqm.core.TableSampleSpec;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.InfixParser;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses table sampling modifiers.
 */
public class SampledTableParser implements MatchableParser<SampledTable>, InfixParser<TableRef, SampledTable> {
    /**
     * Creates a sampled table parser.
     */
    public SampledTableParser() {
    }

    /**
     * Parses standalone sampled table syntax.
     *
     * @param cur cursor
     * @param ctx parse context
     * @return error because sampling requires a source relation
     */
    @Override
    public ParseResult<? extends SampledTable> parse(Cursor cur, ParseContext ctx) {
        return error("Table sampling requires a source table", cur.fullPos());
    }

    /**
     * Parses a sampling modifier after a source relation.
     *
     * @param source source table reference
     * @param cur cursor positioned at the sampling keyword
     * @param ctx parse context
     * @return sampled table
     */
    @Override
    public ParseResult<SampledTable> parse(TableRef source, Cursor cur, ParseContext ctx) {
        if (!ctx.capabilities().supports(SqlFeature.TABLE_SAMPLE)) {
            return error("Table sampling is not supported by this dialect", cur.fullPos());
        }
        var prefix = parseSample(cur);
        var unit = prefix.unit();

        cur.expect("Expected '(' after table sample", TokenType.LPAREN);
        var amount = ctx.parse(Expression.class, cur);
        if (amount.isError()) {
            return error(amount);
        }
        unit = parseSampleUnit(cur, unit);
        cur.expect("Expected ')' after table sample amount", TokenType.RPAREN);

        Expression seed = null;
        if (consumeSeedKeyword(cur)) {
            cur.expect("Expected '(' before table sample seed", TokenType.LPAREN);
            var seedResult = ctx.parse(Expression.class, cur);
            if (seedResult.isError()) {
                return error(seedResult);
            }
            seed = seedResult.value();
            cur.expect("Expected ')' after table sample seed", TokenType.RPAREN);
        }

        return ok(SampledTable.of(
            source,
            TableSampleSpec.of(prefix.method(), unit, amount.value(), seed),
            parseAliasIdentifier(cur)
        ));
    }

    /**
     * Parses the dialect-specific table sampling introducer.
     *
     * @param cur cursor positioned at the sampling keyword
     * @return parsed sampling prefix
     */
    protected SamplePrefix parseSample(Cursor cur) {
        cur.expect("Expected TABLESAMPLE", TokenType.TABLESAMPLE);
        return samplePrefix(parseTableSampleMethod(cur), TableSampleSpec.SampleUnit.UNSPECIFIED);
    }

    /**
     * Parses the dialect-specific sample unit suffix inside the amount parentheses.
     *
     * @param cur cursor positioned after the amount expression
     * @param defaultUnit unit selected by the sampling introducer
     * @return parsed sample unit
     */
    protected TableSampleSpec.SampleUnit parseSampleUnit(Cursor cur, TableSampleSpec.SampleUnit defaultUnit) {
        if (cur.consumeIf(TokenType.PERCENT)) {
            return TableSampleSpec.SampleUnit.PERCENT;
        }
        if (cur.consumeIf(TokenType.ROWS)) {
            return TableSampleSpec.SampleUnit.ROWS;
        }
        return defaultUnit;
    }

    /**
     * Consumes a dialect-specific repeatable seed keyword when present.
     *
     * @param cur cursor positioned after the amount parentheses
     * @return {@code true} if a seed keyword was consumed
     */
    protected boolean consumeSeedKeyword(Cursor cur) {
        return cur.consumeIf(TokenType.REPEATABLE);
    }

    /**
     * Determines whether table sampling starts at the current token.
     *
     * @param cur cursor
     * @param ctx parse context
     * @return {@code true} when sampling syntax starts here
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.TABLESAMPLE);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return sampled table type
     */
    @Override
    public Class<SampledTable> targetType() {
        return SampledTable.class;
    }

    /**
     * Parses an optional TABLESAMPLE method.
     *
     * @param cur cursor positioned after TABLESAMPLE
     * @return parsed sample method, or {@link TableSampleSpec.SampleMethod#SYSTEM} when omitted
     */
    protected static TableSampleSpec.SampleMethod parseTableSampleMethod(Cursor cur) {
        if (cur.consumeIf(TokenType.BERNOULLI)) {
            return TableSampleSpec.SampleMethod.BERNOULLI;
        }
        if (cur.consumeIf(TokenType.SYSTEM)) {
            return TableSampleSpec.SampleMethod.SYSTEM;
        }
        return TableSampleSpec.SampleMethod.SYSTEM;
    }

    /**
     * Creates a parsed table sampling prefix.
     *
     * @param method sampling method
     * @param unit default sampling unit
     * @return parsed sampling prefix
     */
    protected static SamplePrefix samplePrefix(TableSampleSpec.SampleMethod method, TableSampleSpec.SampleUnit unit) {
        return new SamplePrefix(method, unit);
    }

    /**
     * Parsed table sampling prefix.
     *
     * @param method sampling method
     * @param unit default sampling unit
     */
    protected record SamplePrefix(TableSampleSpec.SampleMethod method, TableSampleSpec.SampleUnit unit) {
    }
}
