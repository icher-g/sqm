package io.sqm.parser.oracle;

import io.sqm.core.TableSampleSpec;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;

/**
 * Parses Oracle table sampling syntax.
 */
public class SampledTableParser extends io.sqm.parser.ansi.SampledTableParser {
    /**
     * Creates an Oracle sampled-table parser.
     */
    public SampledTableParser() {
    }

    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.SAMPLE);
    }

    @Override
    protected SamplePrefix parseSample(Cursor cur) {
        cur.expect("Expected SAMPLE", TokenType.SAMPLE);
        var method = cur.consumeIf(TokenType.BLOCK)
            ? TableSampleSpec.SampleMethod.BLOCK
            : TableSampleSpec.SampleMethod.DIALECT_DEFAULT;
        return samplePrefix(method, TableSampleSpec.SampleUnit.PERCENT);
    }

    @Override
    protected TableSampleSpec.SampleUnit parseSampleUnit(Cursor cur, TableSampleSpec.SampleUnit defaultUnit) {
        return defaultUnit;
    }

    @Override
    protected boolean consumeSeedKeyword(Cursor cur) {
        return cur.consumeIf(TokenType.SEED);
    }
}
