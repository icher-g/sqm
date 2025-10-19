package io.sqm.parser.ansi.arg;

import io.sqm.core.FunctionColumn;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * A parser used to parse a function call as an argument of another function.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     SELECT UPPER(CONCAT(customer.name, 'com'));
 *     }
 * </pre>
 */
public class ArgFunctionParser implements Parser<FunctionColumn.Arg.Function> {
    /**
     * Parses function call as an argument inside another function.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context.
     * @return {@link FunctionColumn.Arg.Function}.
     */
    @Override
    public ParseResult<FunctionColumn.Arg.Function> parse(Cursor cur, ParseContext ctx) {
        var fr = ctx.parse(FunctionColumn.class, cur);
        if (fr.isError()) {
            return error(fr); // <— no alias, no EOF check here
        }
        return ok(new FunctionColumn.Arg.Function(fr.value()));
    }

    /**
     * Gets {@link FunctionColumn.Arg.Function} as a target type.
     *
     * @return {@link FunctionColumn.Arg.Function}.
     */
    @Override
    public Class<FunctionColumn.Arg.Function> targetType() {
        return FunctionColumn.Arg.Function.class;
    }
}
