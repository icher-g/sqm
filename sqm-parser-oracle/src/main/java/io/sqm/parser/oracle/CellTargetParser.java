package io.sqm.parser.oracle;

import io.sqm.core.CellAddress;
import io.sqm.core.CellTarget;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

/**
 * Parses writable single-cell addresses such as sales['Bike', 2026].
 */
public class CellTargetParser extends io.sqm.parser.ansi.CellTargetParser {
    /**
     * Creates a target parser.
     */
    public CellTargetParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends CellTarget> parse(Cursor cur, ParseContext ctx) {
        ModelGrammar.require(cur, ctx);
        var measure = toIdentifier(cur.expect("Expected writable measure name", TokenType.IDENT));
        if (ctx instanceof ModelParseContext local && local.scope() != null && !local.scope().contains(measure)) {
            return ParseResult.error("Unknown MODEL measure: " + measure.value(), cur.fullPos());
        }
        var addresses = ModelGrammar.list(this, CellAddress.class, cur, ctx, TokenType.LBRACKET, TokenType.RBRACKET);
        return ParseResult.ok(CellTarget.of(measure, addresses));
    }
}
