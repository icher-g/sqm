package io.sqm.parser;

import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;

/**
 * Parses the “atomic” building blocks of a SQL {@link Query}.
 * <p>
 * This parser is responsible only for the lowest-level query forms:
 *
 * <ul>
 *     <li>Parenthesized subqueries: <code>( ... )</code></li>
 *     <li>Simple {@link SelectQuery} instances</li>
 * </ul>
 * <p>
 * It does <strong>not</strong> handle:
 * <ul>
 *     <li>CTE clauses (<code>WITH ...</code>)</li>
 *     <li>Set operations (<code>UNION</code>, <code>INTERSECT</code>, <code>EXCEPT</code>)</li>
 * </ul>
 * <p>
 * Those concerns are handled by higher-level parsers that wrap or compose
 * {@link Query} nodes. This class is meant to be used as the “primary” /
 * “base” layer of query parsing in the same way arithmetic and predicate
 * parsing have primary parsers.
 * <p>
 * Parsing rules:
 * <ol>
 *     <li>If the current token is <code>(</code>, parse a full nested {@link Query} and
 *     require a closing <code>)</code>.</li>
 *     <li>If the current token starts a <code>SELECT</code>, delegate to
 *     {@link SelectQuery} parser.</li>
 *     <li>Otherwise, return an error indicating that a query was expected.</li>
 * </ol>
 */
public class AtomicQueryParser {

    /**
     * Parses an atomic (non-composite) SQL query.
     *
     * @param cur the cursor providing access to the token stream
     * @param ctx the parsing context used to dispatch parsers
     * @return a {@link ParseResult} containing either a {@link Query} instance or an error
     */
    public ParseResult<? extends Query> parse(Cursor cur, ParseContext ctx) {
        // ( subquery )
        if (cur.consumeIf(TokenType.LPAREN)) {
            var result = ctx.parse(Query.class, cur);
            if (result.isError()) {
                return result;
            }
            cur.expect("Expected ')' after subquery", TokenType.RPAREN);
            return result;
        }

        // SELECT ...
        if (cur.match(TokenType.SELECT)) {
            return ctx.parse(SelectQuery.class, cur);
        }

        // No valid atomic query found
        return error("Expected SELECT or '(' to start a query", cur.fullPos());
    }
}
