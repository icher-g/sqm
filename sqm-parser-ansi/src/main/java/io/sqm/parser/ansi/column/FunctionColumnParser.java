package io.sqm.parser.ansi.column;

import io.sqm.core.FunctionColumn;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.List.copyOf;

/**
 * A parser used to parse a function call.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     SELECT UPPER(CONCAT(customer.name, 'com'));
 *     }
 * </pre>
 */
public class FunctionColumnParser implements Parser<FunctionColumn> {

    /**
     * Parses a function call.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<FunctionColumn> parse(Cursor cur, ParseContext ctx) {
        var t = cur.expect("Expected function name", TokenType.IDENT);

        StringBuilder name = new StringBuilder(t.lexeme());
        while (cur.consumeIf(TokenType.DOT) && cur.match(TokenType.IDENT, 1)) {
            name.append('.').append(cur.advance().lexeme());
        }

        // '('
        cur.expect("Expected '(' after function name", TokenType.LPAREN);

        final boolean distinct = cur.consumeIf(TokenType.DISTINCT);
        final Set<TokenType> terminates = Set.of(TokenType.RPAREN, TokenType.COMMA);
        final List<FunctionColumn.Arg> args = new ArrayList<>();

        do {
            var vr = ctx.parse(FunctionColumn.Arg.class, cur.advance(cur.find(terminates)));
            if (vr.isError()) {
                return error(vr);
            }
            args.add(vr.value());
        } while (cur.consumeIf(TokenType.COMMA));

        // ')'
        cur.expect("Expected ')' to close function", TokenType.RPAREN);

        String alias = parseAlias(cur);
        return ok(new FunctionColumn(name.toString(), copyOf(args), distinct, alias));
    }

    /**
     * Gets {@link FunctionColumn} as a target type for this parser.
     *
     * @return {@link FunctionColumn}.
     */
    @Override
    public Class<FunctionColumn> targetType() {
        return FunctionColumn.class;
    }
}
