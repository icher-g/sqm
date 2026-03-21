package io.sqm.parser.sqlserver;

import io.sqm.core.VariableTableRef;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses SQL Server table-variable references such as {@code @audit}.
 */
public class VariableTableRefParser implements MatchableParser<VariableTableRef> {

    /**
     * Creates a SQL Server table-variable parser.
     */
    public VariableTableRefParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<VariableTableRef> parse(Cursor cur, ParseContext ctx) {
        if (!match(cur, ctx)) {
            return error("Expected SQL Server table variable", cur.fullPos());
        }

        cur.advance();
        var name = toIdentifier(cur.expect("Expected SQL Server table-variable name", TokenType.IDENT));
        return ok(VariableTableRef.of(name));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<VariableTableRef> targetType() {
        return VariableTableRef.class;
    }

    /**
     * Performs a look-ahead test to determine whether this parser is applicable
     * at the current cursor position.
     *
     * @param cur the current cursor pointing to the next token to be parsed
     * @param ctx the parsing context providing configuration, helpers and nested parsing
     * @return {@code true} if this parser should be used to parse the upcoming
     * construct, {@code false} otherwise
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.OPERATOR, "@") && cur.match(TokenType.IDENT, 1);
    }
}
