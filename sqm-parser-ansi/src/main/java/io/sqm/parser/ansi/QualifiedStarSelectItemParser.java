package io.sqm.parser.ansi;

import io.sqm.core.QualifiedStarSelectItem;
import io.sqm.core.SelectItem;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class QualifiedStarSelectItemParser implements Parser<QualifiedStarSelectItem> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<QualifiedStarSelectItem> parse(Cursor cur, ParseContext ctx) {
        var t = cur.expect("Expected identifier", TokenType.IDENT);
        cur.expect("Expected '.'", TokenType.DOT);
        cur.expect("Expected '*'", TokenType.STAR);
        return ok(SelectItem.star(t.lexeme()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<QualifiedStarSelectItem> targetType() {
        return QualifiedStarSelectItem.class;
    }
}
