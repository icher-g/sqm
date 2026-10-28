package io.sqm.parser.oracle;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.core.TokenType.*;

/**
 * Parses RULES UPDATE AUTOMATIC ORDER (sales[2026] = 100), or a bare rule list.
 */
public class ModelRulesParser extends io.sqm.parser.ansi.ModelRulesParser {
    /**
     * Creates a rules parser.
     */
    public ModelRulesParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends ModelRules> parse(Cursor cur, ParseContext ctx) {
        ModelGrammar.require(cur, ctx);
        RuleMode mode = RuleMode.UPSERT;
        RuleOrder order = RuleOrder.SEQUENTIAL;
        IterationSpec iteration = null;
        if (cur.consumeIf(RULES)) {
            var explicitMode = ModelGrammar.mode(cur);
            if (explicitMode != null) mode = explicitMode;
            if (cur.match(AUTOMATIC) || cur.match(SEQUENTIAL)) {
                order = cur.advance().type() == AUTOMATIC ? RuleOrder.AUTOMATIC : RuleOrder.SEQUENTIAL;
                cur.expect("Expected ORDER after rule ordering", ORDER);
            }
            if (cur.match(ITERATE)) iteration = ModelGrammar.node(IterationSpec.class, cur, ctx);
        }
        var rules = ModelGrammar.list(this, ModelRule.class, cur, ctx, LPAREN, RPAREN);
        return ParseResult.ok(ModelRules.of(mode, order, iteration, rules));
    }
}
