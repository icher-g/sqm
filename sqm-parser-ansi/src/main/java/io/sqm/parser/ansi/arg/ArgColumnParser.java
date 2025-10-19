package io.sqm.parser.ansi.arg;

import io.sqm.core.Column;
import io.sqm.core.FunctionColumn;
import io.sqm.core.views.Columns;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * A parser used to parse a column as an argument in a function call.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     SELECT UPPER(customer.name)
 *     }
 * </pre>
 */
public class ArgColumnParser implements Parser<FunctionColumn.Arg.Column> {
    /**
     * Parses function argument as a column.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context.
     * @return {@link FunctionColumn.Arg.Column}.
     */
    @Override
    public ParseResult<FunctionColumn.Arg.Column> parse(Cursor cur, ParseContext ctx) {
        var cr = ctx.parse(Column.class, cur);
        if (cr.isError()) {
            return error(cr);
        }
        return ok(new FunctionColumn.Arg.Column(Columns.table(cr.value()).orElse(null), Columns.name(cr.value()).orElse(null)));
    }

    /**
     * Gets {@link FunctionColumn.Arg.Column} as a target type.
     *
     * @return {@link FunctionColumn.Arg.Column}.
     */
    @Override
    public Class<FunctionColumn.Arg.Column> targetType() {
        return FunctionColumn.Arg.Column.class;
    }
}
