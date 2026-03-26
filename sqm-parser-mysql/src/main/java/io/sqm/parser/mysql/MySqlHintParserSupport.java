package io.sqm.parser.mysql;

import io.sqm.core.Expression;
import io.sqm.core.Identifier;
import io.sqm.core.QualifiedName;
import io.sqm.core.StatementHint;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.Token;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Shared parsing helpers for MySQL optimizer hint comments and table hints.
 */
final class MySqlHintParserSupport {
    private MySqlHintParserSupport() {
    }

    static List<StatementHint> parseCommentHints(String text, ParseContext ctx) {
        var cur = Cursor.of(text, ctx.identifierQuoting());
        var hints = new ArrayList<StatementHint>();
        while (!cur.isEof()) {
            var name = cur.expect("Expected hint name", MySqlHintParserSupport::isHintNameToken).lexeme();
            var args = new ArrayList<>();
            if (cur.consumeIf(TokenType.LPAREN)) {
                while (!cur.match(TokenType.RPAREN)) {
                    args.add(parseArg(cur, ctx));
                    cur.consumeIf(TokenType.COMMA);
                }
                cur.expect("Expected ) after hint arguments", TokenType.RPAREN);
            }
            hints.add(StatementHint.of(Identifier.of(name), args.toArray()));
        }
        return List.copyOf(hints);
    }

    static String indexHintName(String typeKeyword, String scopeKeyword) {
        var suffix = "";
        if (scopeKeyword != null && !scopeKeyword.isEmpty()) {
            suffix = switch (scopeKeyword) {
                case "JOIN" -> "_FOR_JOIN";
                case "ORDER_BY" -> "_FOR_ORDER_BY";
                case "GROUP_BY" -> "_FOR_GROUP_BY";
                default -> throw new IllegalArgumentException("Unsupported index-hint scope " + scopeKeyword);
            };
        }
        return typeKeyword + "_INDEX" + suffix;
    }

    private static boolean isHintNameToken(Token token) {
        return token.type() == TokenType.IDENT;
    }

    private static Object parseArg(Cursor cur, ParseContext ctx) {
        var token = cur.peek();
        if (token.type() == TokenType.NUMBER) {
            int mark = cur.mark();
            cur.advance();
            if (cur.match(TokenType.COMMA) || cur.match(TokenType.RPAREN) || cur.match(TokenType.IDENT)) {
                return parseNumericLiteral(token.lexeme());
            }
            cur.restore(mark);
        }
        if (token.type() == TokenType.STRING) {
            int mark = cur.mark();
            cur.advance();
            if (cur.match(TokenType.COMMA) || cur.match(TokenType.RPAREN) || cur.match(TokenType.IDENT)) {
                return Expression.literal(token.lexeme());
            }
            cur.restore(mark);
        }
        if (token.type() == TokenType.IDENT) {
            int mark = cur.mark();
            var parts = new ArrayList<String>();
            parts.add(cur.advance().lexeme());
            while (cur.consumeIf(TokenType.DOT)) {
                parts.add(cur.expect("Expected identifier after . in hint argument", TokenType.IDENT).lexeme());
            }
            if (cur.match(TokenType.COMMA) || cur.match(TokenType.RPAREN)
                || cur.match(TokenType.IDENT) || cur.match(TokenType.NUMBER) || cur.match(TokenType.STRING)) {
                return parts.size() == 1 ? parts.getFirst() : QualifiedName.of(parts);
            }
            cur.restore(mark);
        }

        int end = cur.find(Set.of(TokenType.COMMA, TokenType.RPAREN));
        if (end == cur.size()) {
            throw new IllegalArgumentException("Expected hint argument");
        }
        var exprCursor = cur.advance(end);
        int mark = exprCursor.mark();
        var expression = ctx.parse(Expression.class, exprCursor);
        if (!expression.isError()) {
            return expression.value();
        }
        exprCursor.restore(mark);
        return stringify(exprCursor);
    }

    private static Expression parseNumericLiteral(String text) {
        if (text.matches("-?\\d+")) {
            try {
                return Expression.literal(Long.parseLong(text));
            }
            catch (NumberFormatException ignored) {
                return Expression.literal(new BigDecimal(text));
            }
        }
        return Expression.literal(new BigDecimal(text));
    }

    private static String stringify(Cursor cur) {
        var sb = new StringBuilder();
        while (!cur.isEof()) {
            sb.append(cur.advance().lexeme());
        }
        return sb.toString();
    }
}
