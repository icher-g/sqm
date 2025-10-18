package io.sqm.parser.ansi.value;

import io.sqm.core.Query;
import io.sqm.core.Values;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class SubqueryValueParser implements Parser<Values.Subquery> {
    @Override
    public ParseResult<Values.Subquery> parse(Cursor cur, ParseContext ctx) {
        var query = ctx.parse(Query.class, cur);
        if (query.isError()) {
            return error(query);
        }
        return ok(new Values.Subquery(query.value()));
    }

    @Override
    public Class<Values.Subquery> targetType() {
        return Values.Subquery.class;
    }
}
