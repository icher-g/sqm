package io.sqm.parser.sqlserver;

import io.sqm.core.OutputRowSource;
import io.sqm.core.OutputStarResultItem;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.core.OperatorTokens.isStar;
import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses SQL Server {@code inserted.*} and {@code deleted.*} output items.
 */
public class OutputStarResultItemParser implements MatchableParser<OutputStarResultItem> {

    /**
     * Creates an output-star result-item parser.
     */
    public OutputStarResultItemParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<OutputStarResultItem> parse(Cursor cur, ParseContext ctx) {
        if (!match(cur, ctx)) {
            return error("Expected inserted.* or deleted.* in OUTPUT clause", cur.fullPos());
        }

        var sourceToken = cur.advance();
        var source = switch (sourceToken.lexeme().toLowerCase()) {
            case "inserted" -> OutputRowSource.INSERTED;
            case "deleted" -> OutputRowSource.DELETED;
            default -> null;
        };
        if (source == null) {
            return error("Expected inserted.* or deleted.* in OUTPUT clause", sourceToken.pos());
        }

        cur.expect("Expected '.' after OUTPUT pseudo-row source", TokenType.DOT);
        cur.expect("Expected '*'", token -> isStar(token));
        return ok(OutputStarResultItem.of(source));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<OutputStarResultItem> targetType() {
        return OutputStarResultItem.class;
    }

    /**
     * Performs a look-ahead test to determine whether this parser is applicable
     * at the current cursor position.
     *
     * @param cur the current cursor pointing to the next token to be parsed
     * @param ctx the parsing context providing configuration, helpers and nested parsing
     * @return {@code true} when the cursor is positioned at {@code inserted.*} or {@code deleted.*}
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.IDENT)
            && cur.match(TokenType.DOT, 1)
            && isStar(cur.peek(2))
            && switch (cur.peek().lexeme().toLowerCase()) {
                case "inserted", "deleted" -> true;
                default -> false;
            };
    }
}
