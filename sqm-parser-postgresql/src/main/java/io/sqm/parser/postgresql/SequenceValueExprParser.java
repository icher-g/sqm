package io.sqm.parser.postgresql;

import io.sqm.core.QualifiedName;
import io.sqm.core.SequenceValueExpr;
import io.sqm.core.SequenceValueKind;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.Token;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.ok;

/**
 * PostgreSQL parser for {@code nextval('sequence')} and {@code currval('sequence')}.
 */
public class SequenceValueExprParser extends io.sqm.parser.ansi.SequenceValueExprParser {
    /**
     * Creates a PostgreSQL sequence value expression parser.
     */
    public SequenceValueExprParser() {
    }

    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(this::isSequenceFunctionName)
            && cur.match(TokenType.LPAREN, 1)
            && cur.match(TokenType.STRING, 2)
            && cur.match(TokenType.RPAREN, 3);
    }

    @Override
    public ParseResult<? extends SequenceValueExpr> parse(Cursor cur, ParseContext ctx) {
        var name = cur.expect("Expected sequence function", TokenType.IDENT);
        var kind = sequenceKind(name);
        cur.expect("Expected (", TokenType.LPAREN);
        var sequence = cur.expect("Expected sequence name literal", TokenType.STRING);
        cur.expect("Expected )", TokenType.RPAREN);
        return ok(SequenceValueExpr.of(QualifiedName.of(sequence.lexeme().split("\\.")), kind));
    }

    private boolean isSequenceFunctionName(Token token) {
        return token.type() == TokenType.IDENT && ("nextval".equalsIgnoreCase(token.lexeme()) || "currval".equalsIgnoreCase(token.lexeme()));
    }

    private SequenceValueKind sequenceKind(Token token) {
        return "nextval".equalsIgnoreCase(token.lexeme()) ? SequenceValueKind.NEXT_VALUE : SequenceValueKind.CURRENT_VALUE;
    }
}
