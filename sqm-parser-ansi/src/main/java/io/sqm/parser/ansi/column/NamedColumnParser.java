package io.sqm.parser.ansi.column;

import io.sqm.core.NamedColumn;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * A parser used to parse a column that contains table/name/alias.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     SELECT products.name as product;
 *     }
 * </pre>
 */
public class NamedColumnParser implements Parser<NamedColumn> {

    /**
     * Parses column from the SQL statement.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<NamedColumn> parse(Cursor cur, ParseContext ctx) {
        // 1) Try: table.name or name
        if (cur.match(TokenType.IDENT)) {
            String table = null, name = null;

            // Option A: t1.c1 [alias]
            if (cur.peek(1).type() == TokenType.DOT && cur.peek(2).type() == TokenType.IDENT) {
                table = cur.advance().lexeme();
                cur.advance(); // skip DOT
                name = cur.advance().lexeme();
            } else {
                // Option B: c1 [alias]
                if (cur.matchAny(1, TokenType.AS, TokenType.IDENT, TokenType.EOF)) {
                    name = cur.advance().lexeme();
                }
            }

            var alias = parseAlias(cur);
            return ok(new NamedColumn(name, alias, table));
        }

        return error("Unexpected tokens at the beginning of expr, expected identifier but found: " + cur.peek(), cur.fullPos());
    }

    /**
     * Gets {@link NamedColumn} as a target type for this parser.
     *
     * @return {@link NamedColumn}.
     */
    @Override
    public Class<NamedColumn> targetType() {
        return NamedColumn.class;
    }
}
