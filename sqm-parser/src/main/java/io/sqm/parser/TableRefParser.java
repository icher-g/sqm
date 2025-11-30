package io.sqm.parser;

import io.sqm.core.QueryTable;
import io.sqm.core.Table;
import io.sqm.core.TableRef;
import io.sqm.core.ValuesTable;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchResult;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;

public class TableRefParser implements Parser<TableRef> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends TableRef> parse(Cursor cur, ParseContext ctx) {
        // ( ... ) – could be either a subquery or a parenthesized table ref
        if (cur.match(TokenType.LPAREN)) {
            MatchResult<? extends TableRef> matched = ctx.parseIfMatch(QueryTable.class, cur);
            if (matched.match()) {
                return matched.result();
            }

            if (cur.consumeIf(TokenType.LPAREN)) {
                matched = ctx.parseIfMatch(ValuesTable.class, cur);
                if (matched.match()) {
                    if (matched.result().ok()) {
                        cur.expect("Expected ')' after VALUES", TokenType.RPAREN);
                    }
                    return matched.result();
                }

                matched = ctx.parseIfMatch(Table.class, cur);
                if (matched.match()) {
                    if (matched.result().ok()) {
                        cur.expect("Expected ')' after table reference", TokenType.RPAREN);
                    }
                    return matched.result();
                }
            }

            return error("Unexpected table reference token: " + cur.peek().lexeme(), cur.fullPos());
        }

        MatchResult<? extends TableRef> matched = ctx.parseIfMatch(ValuesTable.class, cur);
        if (matched.match()) {
            return matched.result();
        }

        matched = ctx.parseIfMatch(Table.class, cur);
        if (matched.match()) {
            return matched.result();
        }

        return error("Unexpected table reference token: " + cur.peek().lexeme(), cur.fullPos());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<TableRef> targetType() {
        return TableRef.class;
    }
}
