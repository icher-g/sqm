package io.sqm.parser.oracle;

import io.sqm.core.Identifier;
import io.sqm.core.MainModel;
import io.sqm.core.ModelColumn;
import io.sqm.core.ModelRules;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.List;

import static io.sqm.parser.core.TokenType.*;

/**
 * Parses optional MAIN/PARTITION, required DIMENSION/MEASURES, options, and rules.
 */
public class MainModelParser extends io.sqm.parser.ansi.MainModelParser {
    /**
     * Creates a main-model parser.
     */
    public MainModelParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends MainModel> parse(Cursor cur, ParseContext ctx) {
        ModelGrammar.require(cur, ctx);
        Identifier name = null;
        if (cur.consumeIf(MAIN)) name = toIdentifier(cur.expect("Expected name after MAIN", IDENT));
        List<ModelColumn> partitions = List.of();
        if (cur.consumeIf(PARTITION)) {
            cur.expect("Expected BY after PARTITION", BY);
            partitions = ModelGrammar.list(this, ModelColumn.class, cur, ctx, LPAREN, RPAREN);
        }
        cur.expect("Expected DIMENSION BY in MODEL", DIMENSION);
        cur.expect("Expected BY after DIMENSION", BY);
        var dimensions = ModelGrammar.list(this, ModelColumn.class, cur, ctx, LPAREN, RPAREN);
        cur.expect("Expected MEASURES after DIMENSION BY", MEASURES);
        var measures = ModelGrammar.list(this, ModelColumn.class, cur, ctx, LPAREN, RPAREN);
        var defaults = ctx instanceof ModelParseContext local ? local.options() : ModelGrammar.Options.DEFAULT;
        var options = ModelGrammar.options(cur, defaults);
        var parent = ctx instanceof ModelParseContext local ? local.parent() : ctx;
        var scoped = new ModelParseContext(parent, options, ModelParseScope.of(measures));
        var rules = ModelGrammar.node(ModelRules.class, cur, scoped);
        return ParseResult.ok(MainModel.of(name, partitions, dimensions, measures, options.navigation(), options.uniqueness(), rules));
    }
}
