package io.sqm.parser.ansi.column;

import io.sqm.core.Query;
import io.sqm.core.QueryColumn;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * A parser to parse a full query statement as a column.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     SELECT
 *        name,
 *        (SELECT name FROM table) AS LastName
 *     FROM MyTable;
 *     }
 * </pre>
 */
public class QueryColumnParser implements Parser<QueryColumn> {

    /**
     * Parses full query statement as a column.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<QueryColumn> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected '(' before SELECT subquery", TokenType.LPAREN);

        var subCur = cur.advance(cur.find(TokenType.RPAREN));
        var body = ctx.parse(Query.class, subCur);
        if (body.isError()) {
            return error(body);
        }

        cur.expect("Expected ')' before SELECT subquery", TokenType.RPAREN);

        var alias = parseAlias(cur);
        return ok(new QueryColumn(body.value(), alias));
    }

    /**
     * Gets {@link QueryColumn} as a target type.
     *
     * @return {@link QueryColumn}.
     */
    @Override
    public Class<QueryColumn> targetType() {
        return QueryColumn.class;
    }
}
