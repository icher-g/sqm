package io.sqm.parser.oracle;

import io.sqm.core.*;
import io.sqm.core.oracle.internal.ModelSyntaxChecker;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.List;

import static io.sqm.parser.core.TokenType.*;

/**
 * Parses FOR year_key IN (2025, 2026), tuple lists, and typed IN (SELECT ...) sources.
 */
public class CellForValuesParser extends io.sqm.parser.ansi.CellForValuesParser {
    /**
     * Creates a values-generator parser.
     */
    public CellForValuesParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends CellFor.Values> parse(Cursor cur, ParseContext ctx) {
        ModelGrammar.require(cur, ctx);
        cur.expect("Expected FOR", FOR);
        boolean tuple = cur.consumeIf(LPAREN);
        List<Identifier> dimensions = tuple
            ? parseIdentifierItems(cur, "Expected FOR dimension")
            : List.of(toIdentifier(cur.expect("Expected FOR dimension", IDENT)));
        if (tuple) cur.expect("Expected ) after FOR dimensions", RPAREN);
        ModelGrammar.dimensions(dimensions, cur, ctx);
        cur.expect("Expected IN after FOR dimensions", IN);
        // Each source retains its own registered parser and parenthesis ownership.
        ValueSet values = cur.match(SELECT, 1) || cur.match(WITH, 1)
            ? ModelGrammar.node(QueryExpr.class, cur, ctx)
            : tuple ? ModelGrammar.node(RowListExpr.class, cur, ctx) : ModelGrammar.node(RowExpr.class, cur, ctx);
        var result = CellFor.Values.of(dimensions, values);
        ModelSyntaxChecker.values(result);
        return ParseResult.ok(result);
    }
}
