package io.cherlabs.sqlmodel.parser.core;

import java.util.*;
import java.util.regex.*;

import static io.cherlabs.sqlmodel.parser.core.TokenType.*;

public final class Lexer {
    private final String s;
    private int pos = 0;
    private final int len;

    private static final Pattern NUM = Pattern.compile("(?:\\d+\\.\\d*|\\d*\\.\\d+|\\d+)(?:[eE][+-]?\\d+)?");

    public Lexer(String s) {
        this.s = s;
        this.len = s.length();
    }

    public static List<Token> lexAll(String s) {
        Lexer lx = new Lexer(s);
        List<Token> out = new ArrayList<>();
        for (Token t = lx.next(); t.type() != EOF; t = lx.next()) {
            out.add(t);
        }
        out.add(new Token(EOF, "", s.length()));
        return out;
    }

    public Token next() {
        skipWSandComments();

        if (pos >= len) return new Token(EOF, "", pos);

        int start = pos;
        char c = s.charAt(pos);

        // strings '...'
        if (c == '\'') return readString();

        // parameters
        if (c == '?') {
            pos++;
            return new Token(PARAM_QMARK, "?", start);
        }

        if (c == ':' || c == '@') {
            int j = pos + 1;
            while (j < len && isIdentifierPart(s.charAt(j))) j++;
            if (j > pos + 1) {
                String name = s.substring(pos, j);
                pos = j;
                return new Token(PARAM_NAMED, name, start);
            }
        }

        // punctuation/operators
        switch (c) {
            case '.':
                pos++;
                return new Token(DOT, ".", start);
            case ',':
                pos++;
                return new Token(COMMA, ",", start);
            case '(':
                pos++;
                return new Token(LPAREN, "(", start);
            case ')':
                pos++;
                return new Token(RPAREN, ")", start);
            case '+':
                pos++;
                return new Token(PLUS, "+", start);
            case '-':
                pos++;
                return new Token(MINUS, "-", start);
            case '*':
                pos++;
                return new Token(STAR, "*", start);
            case '/':
                pos++;
                return new Token(SLASH, "/", start);
            case '^':
                pos++;
                return new Token(CARET, "^", start);
            case '=':
                pos++;
                return new Token(EQ, "=", start);
            case '!':
                if (peek() == '=') {
                    pos += 2;
                    return new Token(NEQ2, "!=", start);
                }
                break;
            case '<':
                if (peek() == '>') {
                    pos += 2;
                    return new Token(NEQ1, "<>", start);
                }
                if (peek() == '=') {
                    pos += 2;
                    return new Token(LTE, "<=", start);
                }
                pos++;
                return new Token(LT, "<", start);
            case '>':
                if (peek() == '=') {
                    pos += 2;
                    return new Token(GTE, ">=", start);
                }
                pos++;
                return new Token(GT, ">", start);
            case '"':
                return readQuotedIdentifier();
            case '[':
                return readBracketIdentifier();
            case '`':
                return readBacktickIdentifier();
        }

        // number?
        if (Character.isDigit(c)) {
            Matcher m = NUM.matcher(s).region(pos, len);
            if (m.lookingAt()) {
                String num = m.group();
                pos += num.length();
                return new Token(NUMBER, num, start);
            }
        }

        // identifier/keyword (bare, no quotes; add "quoted" support if needed)
        if (isIdentifierStart(c)) {
            int j = pos + 1;
            while (j < len && isIdentifierPart(s.charAt(j))) j++;
            String identifier = s.substring(pos, j);
            pos = j;
            TokenType kw = keywordOf(identifier);
            return new Token(kw != null ? kw : IDENT, identifier, start);
        }

        throw new ParserException("Unexpected character '" + c + "'", start);
    }

    private Token readString() {
        int start = pos;
        pos++;
        StringBuilder sb = new StringBuilder();
        while (pos < len) {
            char c = s.charAt(pos++);
            if (c == '\'') {
                if (pos < len && s.charAt(pos) == '\'') {
                    sb.append('\'');
                    pos++;
                    continue;
                }
                return new Token(STRING, sb.toString(), start);
            } else sb.append(c);
        }
        throw new ParserException("Unterminated string literal", start);
    }

    private void skipWSandComments() {
        while (pos < len) {
            char c = s.charAt(pos);
            if (Character.isWhitespace(c)) {
                pos++;
                continue;
            }
            // -- line comment
            if (c == '-' && pos + 1 < len && s.charAt(pos + 1) == '-') {
                pos += 2;
                while (pos < len && s.charAt(pos) != '\n') pos++;
                continue;
            }
            // /* block comment */
            if (c == '/' && pos + 1 < len && s.charAt(pos + 1) == '*') {
                pos += 2;
                while (pos + 1 < len && !(s.charAt(pos) == '*' && s.charAt(pos + 1) == '/')) pos++;
                if (pos + 1 >= len) throw new ParserException("Unterminated block comment", pos);
                pos += 2;
                continue;
            }
            break;
        }
    }

    private char peek() {
        return (pos + 1 < len) ? s.charAt(pos + 1) : '\0';
    }

    private static boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '$';
    }

    private static boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    private static TokenType keywordOf(String keyword) {
        String k = keyword.toUpperCase(Locale.ROOT);
        return switch (k) {
            case "AND" -> AND;
            case "OR" -> OR;
            case "NOT" -> NOT;
            case "IN" -> IN;
            case "LIKE" -> LIKE;
            case "BETWEEN" -> BETWEEN;
            case "IS" -> IS;
            case "NULL" -> NULL;
            case "TRUE" -> TRUE;
            case "FALSE" -> FALSE;
            case "JOIN" -> JOIN;
            case "INNER" -> INNER;
            case "LEFT" -> LEFT;
            case "RIGHT" -> RIGHT;
            case "FULL" -> FULL;
            case "OUTER" -> OUTER;
            case "CROSS" -> CROSS;
            case "ON" -> ON;
            case "AS" -> AS;
            case "DISTINCT" -> DISTINCT;
            case "ASC" -> ASC;
            case "DESC" -> DESC;
            case "NULLS" -> NULLS;
            case "FIRST" -> FIRST;
            case "LAST" -> LAST;
            case "DEFAULT" -> DEFAULT;
            case "COLLATE" -> COLLATE;
            case "CASE" -> CASE;
            case "WHEN" -> WHEN;
            case "THEN" -> THEN;
            case "ELSE" -> ELSE;
            case "END" -> END;
            default -> null;
        };
    }

    private Token readQuotedIdentifier() {
        final int start = pos; // pos is at the opening '"'
        pos++; // consume opening "
        StringBuilder sb = new StringBuilder();
        while (pos < len) {
            char ch = s.charAt(pos++);
            if (ch == '"') {
                // doubled "" -> literal "
                if (pos < len && s.charAt(pos) == '"') {
                    sb.append('"');
                    pos++; // consume the second "
                    continue;
                }
                // closing quote
                return new Token(TokenType.IDENT, sb.toString(), start);
            }
            sb.append(ch);
        }
        throw new IllegalArgumentException("Unterminated double-quoted identifier starting at " + start);
    }

    private Token readBracketIdentifier() {
        final int start = pos; // pos is at '['
        pos++; // consume '['
        StringBuilder sb = new StringBuilder();
        while (pos < len) {
            char ch = s.charAt(pos++);
            if (ch == ']') {
                // doubled ]] -> literal ]
                if (pos < len && s.charAt(pos) == ']') {
                    sb.append(']');
                    pos++; // consume the second ]
                    continue;
                }
                // closing ]
                return new Token(TokenType.IDENT, sb.toString(), start);
            }
            sb.append(ch);
        }
        throw new IllegalArgumentException("Unterminated bracket-quoted identifier starting at " + start);
    }

    private Token readBacktickIdentifier() {
        final int start = pos; // pos is at '`'
        pos++; // consume '`'
        StringBuilder sb = new StringBuilder();
        while (pos < len) {
            char ch = s.charAt(pos++);
            if (ch == '`') {
                // doubled `` -> literal `
                if (pos < len && s.charAt(pos) == '`') {
                    sb.append('`');
                    pos++; // consume the second `
                    continue;
                }
                // closing `
                return new Token(TokenType.IDENT, sb.toString(), start);
            }
            sb.append(ch);
        }
        throw new IllegalArgumentException("Unterminated backtick-quoted identifier starting at " + start);
    }
}
