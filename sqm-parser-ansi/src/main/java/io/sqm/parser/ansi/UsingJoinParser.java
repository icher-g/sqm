package io.sqm.parser.ansi;

import io.sqm.core.Join;
import io.sqm.core.JoinKind;
import io.sqm.core.TableRef;
import io.sqm.core.UsingJoin;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.InfixParser;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.parser.JoinParser.parseKind;
import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

public class UsingJoinParser implements MatchableParser<UsingJoin>, InfixParser<TableRef, UsingJoin> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<UsingJoin> parse(Cursor cur, ParseContext ctx) {
        // JOIN customers USING (customer_id, region_id);
        JoinKind kind = parseKind(cur);

        // JOIN keyword
        cur.expect("Expected JOIN", TokenType.JOIN);

        var table = ctx.parse(TableRef.class, cur);
        if (table.isError()) {
            return error(table);
        }

        var join = parse(table.value(), cur, ctx);
        if (join.isError()) {
            return error(join);
        }
        return ok(join.value().ofKind(kind));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<UsingJoin> targetType() {
        return UsingJoin.class;
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
        return cur.match(TokenType.USING);
    }

    /**
     * Parses a binary operator occurrence where the left-hand side operand
     * has already been parsed.
     *
     * <p>The cursor is positioned at the operator token when this method
     * is invoked. Implementations are responsible for consuming the operator
     * token, parsing the right-hand side operand, and constructing the
     * resulting node.</p>
     *
     * @param lhs the already parsed left-hand operand
     * @param cur the cursor positioned at the operator token
     * @param ctx the parse context
     * @return the parsing result representing {@code lhs <op> rhs}
     */
    @Override
    public ParseResult<UsingJoin> parse(TableRef lhs, Cursor cur, ParseContext ctx) {
        cur.expect("Expected USING", TokenType.USING);
        cur.expect("Expected (", TokenType.LPAREN);

        final List<String> columns = new ArrayList<>();
        do {
            var column = cur.advance().lexeme();
            columns.add(column);
        } while (cur.consumeIf(TokenType.COMMA));

        cur.expect("Expected )", TokenType.RPAREN);
        return ok(Join.inner(lhs).using(columns));
    }
}
