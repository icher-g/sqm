package io.sqm.parser.spi;

import io.sqm.core.Node;
import io.sqm.core.repos.Handler;
import io.sqm.core.utils.Pair;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * A base interface for all spec parsers.
 *
 * @param <T> the type of the entity.
 */
public interface Parser<T extends Node> extends Handler<T> {
    /**
     * Parses a string into a number. Double or Long.
     *
     * @param lexeme a string to parse.
     * @return If the parsing is successful then double or long is returned; if not then the original string is returned.
     */
    default Object parseNumber(String lexeme) {
        try {
            if (lexeme.contains(".") || lexeme.contains("e") || lexeme.contains("E")) {
                return Double.valueOf(lexeme);
            }
            return Long.valueOf(lexeme);
        } catch (NumberFormatException nfe) {
            // fallback to string if something exotic slips through
            return lexeme;
        }
    }

    /**
     * Parses an alias if available.
     *
     * @param cur a cursor containing tokens to parse.
     * @return an alias if exists or null otherwise.
     */
    default String parseAlias(Cursor cur) {
        if (cur.consumeIf(TokenType.AS)) {
            return cur.expect("Expected alias after AS", TokenType.IDENT).lexeme();
        }
        if (cur.match(TokenType.IDENT)) {
            return cur.advance().lexeme();
        }
        // no alias
        return null;
    }

    default Pair<String, List<String>> parseColumnAliases(Cursor cur) {
        String alias = null;
        List<String> columnNames = null;

        if (cur.consumeIf(TokenType.AS) || cur.match(TokenType.IDENT)) {
            alias = cur.expect("Expected identifier", TokenType.IDENT).lexeme();
            if (cur.consumeIf(TokenType.LPAREN)) {
                columnNames = new ArrayList<>();
                do {
                    var column = cur.advance().lexeme();
                    columnNames.add(column);
                } while (cur.consumeIf(TokenType.COMMA));
                cur.expect("Expected )", TokenType.RPAREN);
            }
        }

        return new Pair<>(alias, columnNames);
    }

    /**
     * Parses a list of items delimited by comma.
     *
     * @param clazz a type of the item.
     * @param cur   a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx   a parser context containing parsers and lookups.
     * @return a list of parsed items.
     */
    default <R extends Node, C extends R> ParseResult<List<R>> parseItems(Class<C> clazz, Cursor cur, ParseContext ctx) {
        List<R> items = new ArrayList<>();
        do {
            var result = ctx.parse(clazz, cur);
            if (!result.ok()) {
                return error(result);
            }
            items.add(result.value());
        } while (cur.consumeIf(TokenType.COMMA));
        return ok(items);
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    ParseResult<? extends T> parse(Cursor cur, ParseContext ctx);
}
