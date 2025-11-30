package io.sqm.parser.ansi;

import io.sqm.core.Query;
import io.sqm.core.QueryTable;
import io.sqm.core.TableRef;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.Lookahead;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

public class QueryTableParser implements MatchableParser<QueryTable> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<QueryTable> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected (", TokenType.LPAREN);

        var query = ctx.parse(Query.class, cur);
        if (query.isError()) {
            return error(query);
        }

        cur.expect("Expected )", TokenType.RPAREN);

        var alias = parseAlias(cur);
        return ok(TableRef.query(query.value()).as(alias));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<QueryTable> targetType() {
        return QueryTable.class;
    }

    /**
     * Performs a look-ahead test to determine whether this parser is applicable
     * at the current cursor position.
     * <p>
     * Implementations must <strong>not</strong> advance the cursor or modify
     * the {@link ParseContext}. Their sole responsibility is to inspect the
     * upcoming tokens and decide if this parser is responsible for them.
     *
     * @param cur the cursor pointing at the current token
     * @param ctx the parsing context providing configuration and utilities
     * @return {@code true} if this parser should be used to parse the upcoming
     * input, {@code false} otherwise
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        Lookahead p = Lookahead.initial();
        while (cur.match(TokenType.LPAREN, p.current())) {
            p.increment();
        }
        return cur.match(TokenType.WITH, p.current()) || cur.match(TokenType.SELECT, p.current());
    }
}
