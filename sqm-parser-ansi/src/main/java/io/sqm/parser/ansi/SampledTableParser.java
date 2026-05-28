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

        TableSampleSpec.SampleMethod method = TableSampleSpec.SampleMethod.DIALECT_DEFAULT;
        TableSampleSpec.SampleUnit unit = TableSampleSpec.SampleUnit.UNSPECIFIED;

        if (cur.consumeIf(TokenType.SAMPLE)) {
            unit = TableSampleSpec.SampleUnit.PERCENT;
            if (cur.consumeIf(TokenType.BLOCK)) {
                method = TableSampleSpec.SampleMethod.BLOCK;
            }
        }
        else {
            cur.expect("Expected TableSampleSpec", TokenType.TABLESAMPLE);
            method = parseTableSampleMethod(cur);
        }

        cur.expect("Expected '(' after table sample", TokenType.LPAREN);
        var amount = ctx.parse(Expression.class, cur);
        if (amount.isError()) {
            return error(amount);
        }
        if (cur.consumeIf(TokenType.PERCENT)) {
            unit = TableSampleSpec.SampleUnit.PERCENT;
        }
        else if (cur.consumeIf(TokenType.ROWS)) {
            unit = TableSampleSpec.SampleUnit.ROWS;
        }
        cur.expect("Expected ')' after table sample amount", TokenType.RPAREN);

        Expression seed = null;
        if (cur.consumeIf(TokenType.SEED) || cur.consumeIf(TokenType.REPEATABLE)) {
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
            TableSampleSpec.of(method, unit, amount.value(), seed),
            parseAliasIdentifier(cur)
        ));
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
        return cur.match(TokenType.SAMPLE) || cur.match(TokenType.TABLESAMPLE);
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

    private static TableSampleSpec.SampleMethod parseTableSampleMethod(Cursor cur) {
        if (cur.consumeIf(TokenType.BERNOULLI)) {
            return TableSampleSpec.SampleMethod.BERNOULLI;
        }
        if (cur.consumeIf(TokenType.SYSTEM)) {
            return TableSampleSpec.SampleMethod.SYSTEM;
        }
        return TableSampleSpec.SampleMethod.SYSTEM;
    }
}
