package io.sqm.parser.ansi.arg;

import io.sqm.core.FunctionColumn;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * A parser used to parse '*' as an argument in a function call.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     SELECT COUNT(*);
 *     }
 * </pre>
 */
public class ArgStarParser implements Parser<FunctionColumn.Arg.Star> {
    /**
     * Parses function argument as a '*'.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context.
     * @return {@link FunctionColumn.Arg.Star}
     */
    @Override
    public ParseResult<FunctionColumn.Arg.Star> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expect *", TokenType.STAR);
        return ok(new FunctionColumn.Arg.Star());
    }

    /**
     * Gets {@link FunctionColumn.Arg.Star} as a target type.
     *
     * @return {@link FunctionColumn.Arg.Star} class.
     */
    @Override
    public Class<FunctionColumn.Arg.Star> targetType() {
        return FunctionColumn.Arg.Star.class;
    }
}
