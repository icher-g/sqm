package io.sqm.parser.oracle;

import io.sqm.core.ModelColumn;
import io.sqm.core.Query;
import io.sqm.core.ReferenceModel;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.core.TokenType.*;

/**
 * Parses REFERENCE rates ON (SELECT code, rate FROM fx) DIMENSION BY (code) MEASURES (rate).
 */
public class ReferenceModelParser extends io.sqm.parser.ansi.ReferenceModelParser {
    /**
     * Creates a reference-model parser.
     */
    public ReferenceModelParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends ReferenceModel> parse(Cursor cur, ParseContext ctx) {
        ModelGrammar.require(cur, ctx);
        cur.expect("Expected REFERENCE", REFERENCE);
        var name = toIdentifier(cur.expect("Expected reference-model name", IDENT));
        cur.expect("Expected ON after reference-model name", ON);
        cur.expect("Expected ( before reference query", LPAREN);
        var query = ModelGrammar.node(Query.class, cur, ctx);
        cur.expect("Expected ) after reference query", RPAREN);
        cur.expect("Expected DIMENSION", DIMENSION);
        cur.expect("Expected BY after DIMENSION", BY);
        var dimensions = ModelGrammar.list(this, ModelColumn.class, cur, ctx, LPAREN, RPAREN);
        cur.expect("Expected MEASURES", MEASURES);
        var measures = ModelGrammar.list(this, ModelColumn.class, cur, ctx, LPAREN, RPAREN);
        var defaults = ctx instanceof ModelParseContext local ? local.options() : ModelGrammar.Options.DEFAULT;
        var options = ModelGrammar.options(cur, defaults);
        return ParseResult.ok(ReferenceModel.of(name, query, dimensions, measures, options.navigation(), options.uniqueness()));
    }
}
