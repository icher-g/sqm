package io.sqm.parser;

import io.sqm.core.StatementSequence;
import io.sqm.core.Statement;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry-point parser for statement sequences containing multiple statements.
 */
public class StatementSequenceParser implements Parser<StatementSequence> {

    /**
     * Creates a statement sequence parser.
     */
    public StatementSequenceParser() {
    }

    @Override
    public ParseResult<? extends StatementSequence> parse(Cursor cur, ParseContext ctx) {
        List<Statement> statements = new ArrayList<>();

        while (!cur.isEof()) {
            if (cur.consumeIf(TokenType.SEMICOLON)) {
                continue;
            }

            var statement = ctx.parse(Statement.class, cur);
            if (statement.isError()) {
                return ParseResult.error(statement);
            }

            statements.add(statement.value());
            //noinspection StatementWithEmptyBody
            while (cur.consumeIf(TokenType.SEMICOLON)) {
                // Repeated separators represent empty statements and are ignored.
            }
        }

        return ParseResult.ok(StatementSequence.of(statements));
    }

    @Override
    public Class<StatementSequence> targetType() {
        return StatementSequence.class;
    }
}
