package io.sqm.parser.oracle;

import io.sqm.core.MainModel;
import io.sqm.core.ModelClause;
import io.sqm.core.ReferenceModel;
import io.sqm.core.ReturnRows;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.List;

import static io.sqm.parser.core.TokenType.*;

/**
 * Parses MODEL options followed by a basic main model.
 */
public class ModelClauseParser extends io.sqm.parser.ansi.ModelClauseParser {
    /**
     * Creates a MODEL clause parser.
     */
    public ModelClauseParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends ModelClause> parse(Cursor cur, ParseContext ctx) {
        ModelGrammar.require(cur, ctx);
        cur.expect("Expected MODEL", MODEL);
        var options = ModelGrammar.options(cur, ModelGrammar.Options.DEFAULT);
        var rows = ReturnRows.ALL;
        if (cur.consumeIf(RETURN)) {
            rows = cur.expect("Expected ALL or UPDATED after RETURN", ALL, UPDATED).type() == ALL
                ? ReturnRows.ALL
                : ReturnRows.UPDATED;
            cur.expect("Expected ROWS after RETURN", ROWS);
        }
        if (cur.match(REFERENCE)) return ParseResult.error(ctx.parse(ReferenceModel.class, cur));
        var local = new ModelParseContext(ctx, options, null);
        var main = ModelGrammar.node(MainModel.class, cur, local);
        return ParseResult.ok(ModelClause.of(rows, List.of(), main));
    }
}
