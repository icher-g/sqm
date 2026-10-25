package io.sqm.parser.oracle;

import io.sqm.core.ColumnExpr;
import io.sqm.core.Expression;
import io.sqm.core.ModelColumn;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

/**
 * Parses a MODEL declaration such as product, sales_year AS year_key, or amount * 2 AS sales.
 */
public class ModelColumnParser extends io.sqm.parser.ansi.ModelColumnParser {
    /**
     * Creates a declaration parser.
     */
    public ModelColumnParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends ModelColumn> parse(Cursor cur, ParseContext ctx) {
        ModelGrammar.require(cur, ctx);
        var expression = ModelGrammar.node(Expression.class, cur, ctx);
        var name = parseAliasIdentifier(cur);
        if (name == null && expression instanceof ColumnExpr column) name = column.name();
        if (name == null) return ParseResult.error("Computed MODEL declarations require an alias", cur.fullPos());
        return ParseResult.ok(ModelColumn.of(expression, name));
    }
}
