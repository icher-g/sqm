package io.sqm.parser.spi;

import io.sqm.core.Identifier;
import io.sqm.core.Node;
import io.sqm.core.QualifiedName;
import io.sqm.core.QuoteStyle;
import io.sqm.core.repos.Handler;
import io.sqm.core.utils.Pair;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.Token;
import io.sqm.parser.core.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
     * Parses an identifier alias if available, preserving quote metadata.
     *
     * @param cur a cursor containing tokens to parse
     * @return an identifier alias if present, otherwise {@code null}
     */
    default Identifier parseAliasIdentifier(Cursor cur) {
        if (cur.consumeIf(TokenType.AS)) {
            return toIdentifier(cur.expect("Expected alias after AS", TokenType.IDENT));
        }
        if (cur.match(TokenType.IDENT)) {
            return toIdentifier(cur.advance());
        }
        return null;
    }

    /**
     * Converts a lexer token into an identifier value preserving quote delimiter style.
     *
     * @param token identifier token
     * @return identifier with quote metadata
     */
    default Identifier toIdentifier(Token token) {
        return Identifier.of(token.lexeme(), switch (token.quoteChar()) {
            case '"' -> QuoteStyle.DOUBLE_QUOTE;
            case '`' -> QuoteStyle.BACKTICK;
            case '[' -> QuoteStyle.BRACKETS;
            case null, default -> QuoteStyle.NONE;
        });
    }

    /**
     * Parses a qualified name from the current cursor position.
     * <p>
     * The cursor must point to the first identifier token. The method consumes
     * {@code IDENT (DOT IDENT)*} and preserves quote metadata for each part.
     *
     * @param cur cursor positioned at the first identifier token
     * @return parsed qualified name
     */
    default QualifiedName parseQualifiedName(Cursor cur) {
        var first = cur.expect("Expected identifier", TokenType.IDENT);
        return parseQualifiedName(toIdentifier(first), cur);
    }

    /**
     * Parses the remaining parts of a qualified name when the first identifier
     * has already been consumed.
     *
     * @param firstPart already parsed first identifier part
     * @param cur       cursor positioned after the first part
     * @return parsed qualified name
     */
    default QualifiedName parseQualifiedName(Identifier firstPart, Cursor cur) {
        var parts = new ArrayList<Identifier>();
        parts.add(Objects.requireNonNull(firstPart, "firstPart"));
        while (cur.match(TokenType.DOT) && cur.match(TokenType.IDENT, 1)) {
            cur.advance(); // dot
            parts.add(toIdentifier(cur.advance()));
        }
        return new QualifiedName(parts);
    }

    /**
     * Parses an alias and an optional derived column alias list preserving quote metadata.
     *
     * @param cur a cursor containing tokens to parse.
     * @return alias identifier and optional column alias identifiers.
     */
    default Pair<Identifier, List<Identifier>> parseColumnAliasIdentifiers(Cursor cur) {
        Identifier alias = null;
        List<Identifier> columnNames = null;

        if (cur.consumeIf(TokenType.AS) || cur.match(TokenType.IDENT)) {
            alias = toIdentifier(cur.expect("Expected identifier", TokenType.IDENT));
            if (cur.consumeIf(TokenType.LPAREN)) {
                columnNames = new ArrayList<>();
                do {
                    var column = cur.expect("Expected identifier", TokenType.IDENT);
                    columnNames.add(toIdentifier(column));
                } while (cur.consumeIf(TokenType.COMMA));
                cur.expect("Expected )", TokenType.RPAREN);
            }
        }

        return new Pair<>(alias, columnNames);
    }

    /**
     * Parses a list of items delimited by comma.
     *
     * @param <R>   supertype of parsed items
     * @param <C>   concrete parser target type
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
