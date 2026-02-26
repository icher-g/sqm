package io.sqm.parser.core;

import io.sqm.parser.spi.IdentifierQuoting;

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
        KEYWORDS.put("ILIKE", ILIKE);
        KEYWORDS.put("SIMILAR", SIMILAR);
        KEYWORDS.put("TO", TO);
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
        KEYWORDS.put("WITHOUT", WITHOUT);
        KEYWORDS.put("RECURSIVE", RECURSIVE);
        KEYWORDS.put("SELECT", SELECT);
        KEYWORDS.put("FROM", FROM);
        KEYWORDS.put("LATERAL", LATERAL);
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
        KEYWORDS.put("INSERT", INSERT);
        KEYWORDS.put("DELETE", DELETE);
        KEYWORDS.put("MERGE", MERGE);
        KEYWORDS.put("TRUNCATE", TRUNCATE);
        KEYWORDS.put("REPLACE", REPLACE);
        KEYWORDS.put("COPY", COPY);
        KEYWORDS.put("CREATE", CREATE);
        KEYWORDS.put("ALTER", ALTER);
        KEYWORDS.put("DROP", DROP);
        KEYWORDS.put("GRANT", GRANT);
        KEYWORDS.put("REVOKE", REVOKE);
        KEYWORDS.put("COMMENT", COMMENT);
        KEYWORDS.put("RENAME", RENAME);
        KEYWORDS.put("MATERIALIZED", MATERIALIZED);
        KEYWORDS.put("ORDINALITY", ORDINALITY);
        KEYWORDS.put("GROUPING", GROUPING);
        KEYWORDS.put("SETS", SETS);
        KEYWORDS.put("ROLLUP", ROLLUP);
        KEYWORDS.put("CUBE", CUBE);
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
        KEYWORDS.put("CAST", CAST);
        KEYWORDS.put("ARRAY", ARRAY);
        KEYWORDS.put("FOR", FOR);
        KEYWORDS.put("UPDATE", UPDATE);
        KEYWORDS.put("SHARE", SHARE);
        KEYWORDS.put("KEY", KEY);
        KEYWORDS.put("OF", OF);
        KEYWORDS.put("NOWAIT", NOWAIT);
        KEYWORDS.put("SKIP", SKIP);
        KEYWORDS.put("LOCKED", LOCKED);
    }

    private final IdentifierQuoting identifierQuoting;
    private final String s;
    private final int len;
    private int pos = 0;

    /**
     * Creates lexer for the provided SQL text.
     *
     * @param s input SQL text
     * @param identifierQuoting dialect-specific identifier quoting rules
     */
    public Lexer(String s, IdentifierQuoting identifierQuoting) {
        this.s = s;
        this.len = s.length();
        this.identifierQuoting = identifierQuoting;
    }

    /**
     * Converts a string into a list of tokens.
     *
     * @param s a string to convert.
        * @param identifierQuoting dialect-specific identifier quoting rules
     * @return a list of tokens.
     */
    public static List<Token> lexAll(String s, IdentifierQuoting identifierQuoting) {
        Lexer lx = new Lexer(s, identifierQuoting);
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

    private static boolean isOperatorChar(char c) {
        return "+-*/<>=~!@#%^&|`?".indexOf(c) >= 0;
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

        // prefixed strings
        if ((c == 'E' || c == 'e') && peekNext() == '\'') {
            pos++; // consume prefix
            return readStringLiteral(TokenType.ESCAPE_STRING, true);
        }
        if ((c == 'B' || c == 'b') && peekNext() == '\'') {
            pos++; // consume prefix
            return readStringLiteral(TokenType.BIT_STRING, false);
        }
        if ((c == 'X' || c == 'x') && peekNext() == '\'') {
            pos++; // consume prefix
            return readStringLiteral(TokenType.HEX_STRING, false);
        }

        // strings '...'
        if (c == '\'') return readString();

        // parameters
        if (c == '?') {
            char n = peekNext();
            if (n == '|' || n == '&') {
                pos += 2;
                return new Token(OPERATOR, "?" + n, start);
            }
            pos++;
            return new Token(QMARK, "?", start);
        }

        // : OR :: tokens
        if (c == ':') {
            if (peekNext() == ':') {
                pos += 2;
                return new Token(DOUBLE_COLON, "::", start);
            }
            pos++;
            return new Token(COLON, ":", start);
        }

        // punctuation/operators
        switch (c) {
            case '$':
                var dollarString = readDollarString();
                if (dollarString != null) {
                    return dollarString;
                }
                pos++;
                return new Token(DOLLAR, "$", start);
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
            case '"':
                if (identifierQuoting.supports('"')) {
                    return readQuotedIdentifier();
                }
                pos++;
                return new Token(QUOTE, "\"", start);
            case '[':
                if (identifierQuoting.supports('[')) {
                    return readBracketIdentifier();
                }
                pos++;
                return new Token(LBRACKET, "[", start);
            case ']':
                pos++;
                return new Token(RBRACKET, "]", start);
            case '`':
                if (identifierQuoting.supports('`')) {
                    return readBacktickIdentifier();
                }
                pos++;
                return new Token(BACKTICK, "`", start);
        }

        // generic operator (custom operators, non-ANSI symbols, etc.)
        if (isOperatorChar(c)) {
            int j = pos;
            while (j < len && isOperatorChar(s.charAt(j))) j++;
            String op = s.substring(pos, j);
            pos = j;
            return new Token(OPERATOR, op, start);
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
        return readStringLiteral(STRING, false);
    }

    private Token readStringLiteral(TokenType type, boolean preserveEscapes) {
        int start = pos;
        pos++; // consume opening quote
        StringBuilder sb = new StringBuilder();
        while (pos < len) {
            char c = s.charAt(pos++);
            if (preserveEscapes && c == '\\') {
                if (pos >= len) {
                    throw new ParserException("Unterminated string literal", start);
                }
                char next = s.charAt(pos++);
                sb.append('\\').append(next);
                continue;
            }
            if (c == '\'') {
                if (pos < len && s.charAt(pos) == '\'') {
                    if (preserveEscapes) {
                        sb.append("''");
                    }
                    else {
                        sb.append('\'');
                    }
                    pos++;
                    continue;
                }
                return new Token(type, sb.toString(), start);
            }
            sb.append(c);
        }
        throw new ParserException("Unterminated string literal", start);
    }

    private Token readDollarString() {
        int start = pos;
        int tagStart = pos + 1;
        if (tagStart >= len) {
            return null;
        }
        char next = s.charAt(tagStart);
        if (Character.isDigit(next)) {
            return null;
        }
        if (next != '$' && !isDollarTagStart(next)) {
            return null;
        }
        int tagEnd = tagStart;
        if (next != '$') {
            while (tagEnd < len && isDollarTagPart(s.charAt(tagEnd))) {
                tagEnd++;
            }
            if (tagEnd >= len || s.charAt(tagEnd) != '$') {
                return null;
            }
        }

        String tag = s.substring(tagStart, tagEnd);
        String delim = "$" + tag + "$";
        int contentStart = tagEnd + 1;
        int end = s.indexOf(delim, contentStart);
        if (end < 0) {
            throw new ParserException("Unterminated dollar-quoted string literal", start);
        }
        String raw = s.substring(start, end + delim.length());
        pos = end + delim.length();
        return new Token(DOLLAR_STRING, raw, start);
    }

    private static boolean isDollarTagStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isDollarTagPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
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

    private char peekNext() {
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
                return new Token(TokenType.IDENT, sb.toString(), start, '"');
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
                return new Token(TokenType.IDENT, sb.toString(), start, '[');
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
                return new Token(TokenType.IDENT, sb.toString(), start, '`');
            }
            sb.append(ch);
        }
        throw new IllegalArgumentException("Unterminated backtick-quoted identifier starting at " + start);
    }
}
