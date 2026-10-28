package io.sqm.parser.oracle;

import io.sqm.core.CellPresentPredicate;
import io.sqm.core.CellRefExpr;
import io.sqm.core.Expression;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.InfixParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.core.TokenType.IS;
import static io.sqm.parser.core.TokenType.PRESENT;

/**
 * Parses sales[2025] IS PRESENT through the dedicated readable-cell parser.
 */
public class CellPresentPredicateParser extends io.sqm.parser.ansi.CellPresentPredicateParser
    implements InfixParser<Expression, CellPresentPredicate> {
    /**
     * Creates a cell-presence predicate parser.
     */
    public CellPresentPredicateParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends CellPresentPredicate> parse(Cursor cur, ParseContext ctx) {
        ModelGrammar.require(cur, ctx);
        return parse(ModelGrammar.node(CellRefExpr.class, cur, ctx), cur, ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<CellPresentPredicate> parse(Expression lhs, Cursor cur, ParseContext ctx) {
        ModelGrammar.require(cur, ctx);
        if (!(lhs instanceof CellRefExpr cell)) return ParseResult.error("IS PRESENT requires a MODEL cell reference", cur.fullPos());
        cur.expect("Expected IS", IS);
        cur.expect("Expected PRESENT after IS", PRESENT);
        return ParseResult.ok(CellPresentPredicate.of(cell));
    }
}
