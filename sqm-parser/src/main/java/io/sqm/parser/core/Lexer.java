package io.sqm.parser.core;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.sqm.parser.core.TokenType.*;

/**
 * This class is used to split the string into a list of tokens.
 */
public final class Lexer {
    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();
    private static final Pattern NUM = Pattern.compile("(?:\\d+\\.\\d*|\\d*\\.\\d+|\\d+)(?:[eE][+-]?\\d+)?");

    static {
        KEYWORDS.put("AND", AND);
        KEYWORDS.put("OR", OR);
        KEYWORDS.put("NOT", NOT);
        KEYWORDS.put("IN", IN);
        KEYWORDS.put("LIKE", LIKE);
        KEYWORDS.put("BETWEEN", BETWEEN);
        KEYWORDS.put("SYMMETRIC", SYMMETRIC);
        KEYWORDS.put("IS", IS);
        KEYWORDS.put("NULL", NULL);
        KEYWORDS.put("TRUE", TRUE);
        KEYWORDS.put("FALSE", FALSE);
        KEYWORDS.put("JOIN", JOIN);
        KEYWORDS.put("INNER", INNER);
        KEYWORDS.put("LEFT", LEFT);
        KEYWORDS.put("RIGHT", RIGHT);
        KEYWORDS.put("FULL", FULL);
        KEYWORDS.put("OUTER", OUTER);
        KEYWORDS.put("CROSS", CROSS);
        KEYWORDS.put("USING", USING);
        KEYWORDS.put("NATURAL", NATURAL);
        KEYWORDS.put("ON", ON);
        KEYWORDS.put("AS", AS);
        KEYWORDS.put("DISTINCT", DISTINCT);
        KEYWORDS.put("EXISTS", EXISTS);
        KEYWORDS.put("ESCAPE", ESCAPE);
        KEYWORDS.put("ANY", ANY);
        KEYWORDS.put("ASC", ASC);
        KEYWORDS.put("DESC", DESC);
        KEYWORDS.put("NULLS", NULLS);
        KEYWORDS.put("FIRST", FIRST);
        KEYWORDS.put("LAST", LAST);
        KEYWORDS.put("DEFAULT", DEFAULT);
        KEYWORDS.put("COLLATE", COLLATE);
        KEYWORDS.put("CASE", CASE);
        KEYWORDS.put("WHEN", WHEN);
        KEYWORDS.put("THEN", THEN);
        KEYWORDS.put("ELSE", ELSE);
        KEYWORDS.put("END", END);
        KEYWORDS.put("WITH", WITH);
        KEYWORDS.put("RECURSIVE", RECURSIVE);
        KEYWORDS.put("SELECT", SELECT);
        KEYWORDS.put("FROM", FROM);
        KEYWORDS.put("WHERE", WHERE);
        KEYWORDS.put("GROUP", GROUP);
        KEYWORDS.put("HAVING", HAVING);
        KEYWORDS.put("ORDER", ORDER);
        KEYWORDS.put("LIMIT", LIMIT);
        KEYWORDS.put("OFFSET", OFFSET);
        KEYWORDS.put("FETCH", FETCH);
        KEYWORDS.put("NEXT", NEXT);
        KEYWORDS.put("ROW", ROW);
        KEYWORDS.put("ROWS", ROWS);
        KEYWORDS.put("ONLY", ONLY);
        KEYWORDS.put("BY", BY);
        KEYWORDS.put("TOP", TOP);
        KEYWORDS.put("UNION", UNION);
        KEYWORDS.put("INTERSECT", INTERSECT);
        KEYWORDS.put("EXCEPT", EXCEPT);
        KEYWORDS.put("ALL", ALL);
        KEYWORDS.put("VALUES", VALUES);
        KEYWORDS.put("WINDOW", WINDOW);
        KEYWORDS.put("OVER", OVER);
        KEYWORDS.put("PARTITION", PARTITION);
        KEYWORDS.put("RANGE", RANGE);
        KEYWORDS.put("GROUPS", GROUPS);
        KEYWORDS.put("UNBOUNDED", UNBOUNDED);
        KEYWORDS.put("PRECEDING", PRECEDING);
        KEYWORDS.put("FOLLOWING", FOLLOWING);
        KEYWORDS.put("CURRENT", CURRENT);
        KEYWORDS.put("EXCLUDE", EXCLUDE);
        KEYWORDS.put("TIES", TIES);
        KEYWORDS.put("NO", NO);
        KEYWORDS.put("OTHERS", OTHERS);
        KEYWORDS.put("WITHIN", WITHIN);
        KEYWORDS.put("FILTER", FILTER);
    }

    private final String s;
    private final int len;
    private int pos = 0;

    public Lexer(String s) {
        this.s = s;
        this.len = s.length();
    }

    /**
     * Converts a string into a list of tokens.
     *
     * @param s a string to convert.
     * @return a list of tokens.
     */
    public static List<Token> lexAll(String s) {
        Lexer lx = new Lexer(s);
        List<Token> out = new ArrayList<>();
        for (Token t = lx.next(); t.type() != EOF; t = lx.next()) {
            out.add(t);
        }
        out.add(new Token(EOF, "", s.length()));
        return out;
    }

    private static boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '$';
    }

    private static boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    private static TokenType keywordOf(String keyword) {
        String k = keyword.toUpperCase(Locale.ROOT);
        return KEYWORDS.get(k);
    }

    /**
     * Gets a next token.
     *
     * @return a token.
     */
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

        if (c == '$') {
            int j = pos + 1;
            if (Character.isDigit(s.charAt(j))) {
                Matcher m = NUM.matcher(s).region(j, len);
                if (m.lookingAt()) {
                    String num = m.group();
                    pos = j + num.length();
                    return new Token(PARAM_POS, num, start);
                }
            }
        }

        if (c == ':' || c == '@') {
            int j = pos + 1;
            while (j < len && isIdentifierPart(s.charAt(j))) j++;
            if (j > pos + 1) {
                String name = s.substring(pos + 1, j);
                // parameter cannot start with a digit.
                if (!Character.isDigit(name.charAt(0))) {
                    pos = j;
                    return new Token(PARAM_NAMED, name, start);
                }
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
            }
            else sb.append(c);
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
