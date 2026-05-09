package io.sqm.parser.ansi;

import io.sqm.core.HierarchicalQueryClause;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;

/**
 * Placeholder parser for hierarchical query clauses in dialects that do not support them.
 */
public class HierarchicalQueryClauseParser implements MatchableParser<HierarchicalQueryClause> {
    /**
     * Creates an ANSI hierarchical-query-clause parser.
     */
    public HierarchicalQueryClauseParser() {
    }

    /**
     * Returns an unsupported feature parse result.
     *
     * @param cur token cursor
     * @param ctx parse context
     * @return unsupported feature result
     */
    @Override
    public ParseResult<? extends HierarchicalQueryClause> parse(Cursor cur, ParseContext ctx) {
        return error("Hierarchical queries are not supported by this dialect", cur.fullPos());
    }

    /**
     * Performs a look-ahead test for hierarchical query syntax.
     *
     * @param cur token cursor
     * @param ctx parse context
     * @return {@code true} when hierarchical query syntax starts at the current cursor
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.START)
            || cur.match(TokenType.CONNECT)
            || (cur.match(TokenType.ORDER) && cur.match(TokenType.SIBLINGS, 1));
    }

    /**
     * Returns this parser target type.
     *
     * @return {@link HierarchicalQueryClause} class
     */
    @Override
    public Class<HierarchicalQueryClause> targetType() {
        return HierarchicalQueryClause.class;
    }
}
