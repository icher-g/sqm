package io.sqm.parser.oracle;

import io.sqm.core.ColumnExpr;
import io.sqm.core.Expression;
import io.sqm.core.IsAnyPredicate;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.InfixParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.core.TokenType.ANY;
import static io.sqm.parser.core.TokenType.IS;

/**
 * Parses bare ANY and dimension wildcards such as product IS ANY.
 */
public class IsAnyPredicateParser extends io.sqm.parser.ansi.IsAnyPredicateParser
    implements InfixParser<Expression, IsAnyPredicate> {
    /**
     * Creates a MODEL wildcard parser.
     */
    public IsAnyPredicateParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends IsAnyPredicate> parse(Cursor cur, ParseContext ctx) {
        ModelGrammar.require(cur, ctx);
        if (cur.consumeIf(ANY)) return ParseResult.ok(IsAnyPredicate.of(null));
        return parse(ModelGrammar.node(Expression.class, cur, ctx), cur, ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<IsAnyPredicate> parse(Expression lhs, Cursor cur, ParseContext ctx) {
        ModelGrammar.require(cur, ctx);
        if (!(lhs instanceof ColumnExpr column) || column.tableAlias() != null) {
            return ParseResult.error("IS ANY requires an unqualified MODEL dimension", cur.fullPos());
        }
        cur.expect("Expected IS", IS);
        cur.expect("Expected ANY after IS", ANY);
        return ParseResult.ok(IsAnyPredicate.of(lhs));
    }
}
