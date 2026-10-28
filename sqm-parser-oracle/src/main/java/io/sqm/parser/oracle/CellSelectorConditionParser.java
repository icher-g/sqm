package io.sqm.parser.oracle;

import io.sqm.core.CellSelector;
import io.sqm.core.IsAnyPredicate;
import io.sqm.core.Predicate;
import io.sqm.core.oracle.internal.ModelSyntaxChecker;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

/**
 * Parses symbolic selectors such as year_key BETWEEN 2025 AND 2027, product IS ANY, or ANY.
 */
public class CellSelectorConditionParser extends io.sqm.parser.ansi.CellSelectorConditionParser {
    /**
     * Creates a symbolic-selector parser.
     */
    public CellSelectorConditionParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends CellSelector.Condition> parse(Cursor cur, ParseContext ctx) {
        ModelGrammar.require(cur, ctx);
        Predicate predicate = cur.match(TokenType.ANY)
            ? ModelGrammar.node(IsAnyPredicate.class, cur, ctx)
            : ModelGrammar.node(Predicate.class, cur, ctx);
        ModelSyntaxChecker.selector(predicate);
        return ParseResult.ok(CellSelector.Condition.of(predicate));
    }
}
