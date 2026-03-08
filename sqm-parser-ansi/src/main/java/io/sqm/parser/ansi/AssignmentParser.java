package io.sqm.parser.ansi;

import io.sqm.core.Assignment;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses baseline ANSI {@code column = expr} update assignments.
 */
public class AssignmentParser implements Parser<Assignment> {

    /**
     * Creates an assignment parser.
     */
    public AssignmentParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends Assignment> parse(Cursor cur, ParseContext ctx) {
        var column = toIdentifier(cur.expect("Expected assignment target column", TokenType.IDENT));
        var eq = cur.expect("Expected = in assignment", TokenType.OPERATOR);
        if (!"=".equals(eq.lexeme())) {
            return error("Expected = in assignment", eq.pos());
        }

        var value = ctx.parse(io.sqm.core.Expression.class, cur);
        if (value.isError()) {
            return error(value);
        }

        return ok(Assignment.of(column, value.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<Assignment> targetType() {
        return Assignment.class;
    }
}
