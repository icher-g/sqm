package io.sqm.parser.oracle;

import io.sqm.core.CellTarget;
import io.sqm.core.Expression;
import io.sqm.core.ModelRule;
import io.sqm.core.OrderBy;
import io.sqm.core.oracle.internal.ModelSyntaxChecker;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.OperatorTokens;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

/**
 * Parses an assignment such as UPDATE sales[2026] = sales[2025] * 1.1.
 */
public class ModelRuleParser extends io.sqm.parser.ansi.ModelRuleParser {
    /**
     * Creates a rule parser.
     */
    public ModelRuleParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends ModelRule> parse(Cursor cur, ParseContext ctx) {
        ModelGrammar.require(cur, ctx);
        var mode = ModelGrammar.mode(cur);
        var target = ModelGrammar.node(CellTarget.class, cur, ctx);
        var order = cur.match(TokenType.ORDER) ? ModelGrammar.node(OrderBy.class, cur, ctx) : null;
        ModelSyntaxChecker.orderBy(order);
        cur.expect("Expected = after MODEL cell target", OperatorTokens::isEq);
        var value = ModelGrammar.node(Expression.class, cur, ctx);
        ModelSyntaxChecker.scalar(value);
        return ParseResult.ok(ModelRule.of(mode, target, order, value));
    }
}
