package io.sqm.parser.oracle;

import io.sqm.core.Identifier;
import io.sqm.core.QualifiedName;
import io.sqm.core.SequenceValueExpr;
import io.sqm.core.SequenceValueKind;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.ArrayList;

import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Oracle parser for {@code sequence.NEXTVAL} and {@code sequence.CURRVAL}.
 */
public class SequenceValueExprParser extends io.sqm.parser.ansi.SequenceValueExprParser {
    /**
     * Creates an Oracle sequence value expression parser.
     */
    public SequenceValueExprParser() {
    }

    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        if (!cur.match(TokenType.IDENT) || !cur.match(TokenType.DOT, 1) || !cur.match(TokenType.IDENT, 2)) {
            return false;
        }
        int offset = 2;
        while (cur.match(TokenType.DOT, offset + 1) && cur.match(TokenType.IDENT, offset + 2)) {
            offset += 2;
        }
        var token = cur.peek(offset);
        return token.type() == TokenType.IDENT && ("nextval".equalsIgnoreCase(token.lexeme()) || "currval".equalsIgnoreCase(token.lexeme()));
    }

    @Override
    public ParseResult<? extends SequenceValueExpr> parse(Cursor cur, ParseContext ctx) {
        var parts = new ArrayList<Identifier>();
        parts.add(toIdentifier(cur.expect("Expected sequence name", TokenType.IDENT)));
        while (cur.consumeIf(TokenType.DOT)) {
            parts.add(toIdentifier(cur.expect("Expected sequence name part", TokenType.IDENT)));
        }
        var kind = sequenceKind(parts.removeLast().value());
        return ok(SequenceValueExpr.of(new QualifiedName(parts), kind));
    }

    private SequenceValueKind sequenceKind(String name) {
        return "nextval".equalsIgnoreCase(name) ? SequenceValueKind.NEXT_VALUE : SequenceValueKind.CURRENT_VALUE;
    }
}
