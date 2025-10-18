package io.sqm.parser.core;

import java.util.*;

/**
 * Cursor for token list that maintains current position.
 * All parsers share the same cursor instance to advance naturally.
 */
public final class Cursor {
    private final List<Token> tokens;
    /**
     * position in the cursor this cursor was built from.
     */
    private final int basePos;
    private int pos;


    /**
     * Creates Cursor from the list of tokens.
     *
     * @param tokens a list of tokens.
     */
    public Cursor(List<Token> tokens) {
        this(tokens, 0);
    }

    /**
     * Creates Cursor from the list of tokens and a start point.
     *
     * @param tokens  a list of tokens.
     * @param basePos a position in the cursor this cursor has been built from.
     */
    public Cursor(List<Token> tokens, int basePos) {
        var copy = new ArrayList<>(tokens);
        if (copy.isEmpty() || copy.get(copy.size() - 1).type() != TokenType.EOF) {
            int pos = copy.isEmpty() ? 0 : copy.get(copy.size() - 1).pos();
            copy.add(new Token(TokenType.EOF, "", pos));
        }
        this.tokens = copy;
        this.basePos = basePos;
        this.pos = 0;
    }

    /**
     * Gets a number of tokens.
     *
     * @return a number of tokens.
     */
    public int size() {
        return tokens.size();
    }

    /**
     * Gets a current token.
     *
     * @return a token.
     */
    public Token peek() {
        if (pos >= tokens.size()) throw new NoSuchElementException("No more tokens");
        return tokens.get(pos);
    }

    /**
     * Gets a lookahead token from current position.
     *
     * @param lookahead a number of tokens to lookahead.
     * @return a token.
     */
    public Token peek(int lookahead) {
        if (pos + lookahead >= tokens.size()) throw new NoSuchElementException("No more tokens");
        return tokens.get(pos + lookahead);
    }

    /***
     * Returns current token and advances the cursor.
     * @return current Token
     */
    public Token advance() {
        if (pos >= tokens.size()) throw new NoSuchElementException("No more tokens");
        return tokens.get(pos++);
    }

    /**
     * Slices the Cursor into the new Cursor starting from the current position. A new Cursor will have EOF token at its end.
     *
     * @param end the end position for the slice.
     * @return A new Cursor containing a sub list of tokens.
     */
    public Cursor advance(int end) {
        if (end < pos || end > tokens.size()) {
            throw new IndexOutOfBoundsException("Invalid slice end: " + end);
        }
        var cur = new Cursor(tokens.subList(pos, end), pos);
        pos = end;
        return cur;
    }

    /**
     * Removes '(', ')' brackets if the cursor starts with '(' on the current position and ends with ')' on the last position.
     *
     * @return a new cursor without brackets if they were removed or the same cursor if no brackets were found.
     */
    public Cursor removeBrackets() {
        if (match(TokenType.LPAREN)) {
            var i = find(Set.of(TokenType.RPAREN), 1);
            if (i == tokens.size() - 2) { // last token is always EOR
                advance(); // skip '(' on current cursor.
                var cur = advance(i); // extract tokens in between the brackets.
                advance(); // skip ')' on current cursor.
                return cur;
            }
        }
        return this;
    }

    /**
     * Gets current position.
     *
     * @return current position.
     */
    public int pos() {
        return pos;
    }

    /**
     * Gets the current position of the cursor + the position of the cursor this one has been constructed from.
     *
     * @return a full position.
     */
    public int fullPos() {
        return basePos + pos;
    }

    /**
     * Indicates whether it is the EOF.
     *
     * @return True if the current position is on the EOF or False otherwise.
     */
    public boolean isEof() {
        return match(TokenType.EOF);
    }

    /**
     * Validates if current token type is the same as the provided one.
     *
     * @param type the type to match.
     * @return True if the current token type matches the provided one.
     */
    public boolean match(TokenType type) {
        return peek().type() == type;
    }

    /**
     * Validates if current token type with respect to a lookahead position is the same as the provided one.
     *
     * @param type      the type to match.
     * @param lookahead the lookahead position.
     * @return True if the token type matches the provided one.
     */
    public boolean match(TokenType type, int lookahead) {
        return peek(lookahead).type() == type;
    }

    /**
     * Validates if current token type matches on of the provided token types.
     *
     * @param types the types to match.
     * @return True if one of the provided token types matches the current.
     */
    public boolean matchAny(TokenType... types) {
        var type = peek().type();
        return Arrays.stream(types).anyMatch(tt -> type == tt);
    }

    /**
     * Validates if current token type with respect to a lookahead position matches one of the provided token types.
     *
     * @param lookahead the lookahead position.
     * @param types     the types to match.
     * @return True if one of the provided token types matches the current + lookahead.
     */
    public boolean matchAny(int lookahead, TokenType... types) {
        var type = peek(lookahead).type();
        return Arrays.stream(types).anyMatch(tt -> type == tt);
    }

    /**
     * Validates if the provided token type matches the current token type and advances the cursor if the match is found.
     *
     * @param type the type to match.
     * @return True if the token was consumed or False otherwise.
     */
    public boolean consumeIf(TokenType type) {
        if (match(type)) {
            pos++;
            return true;
        }
        return false;
    }

    /**
     * Expect token; throw otherwise. The cursor is advanced in any case.
     *
     * @param message an error message to throw if the token is not matched.
     * @param types   a list of types to expect.
     */
    public Token expect(String message, TokenType... types) {
        return expect(message, Set.of(types));
    }

    /**
     * Expect token; throw otherwise. The cursor is advanced in any case.
     *
     * @param message an error message to throw if the token is not matched.
     * @param types   a list of types to expect.
     */
    public Token expect(String message, Set<TokenType> types) {
        Token token = advance();
        if (!types.contains(token.type())) {
            throw new ParserException(message, token.pos());
        }
        return token;
    }

    /**
     * Find the index of the first token type at top-level (parenDepth == 0), scanning from 'current position'.
     *
     * @param types a list of types to look for any of them.
     * @return size if not found.
     */
    public int find(TokenType... types) {
        return find(Set.of(types));
    }

    /**
     * Find the index of the first token type at top-level (parenDepth == 0), scanning from 'current position'.
     *
     * @param types a list of types to look for any of them.
     * @return size if not found.
     */
    public int find(Set<TokenType> types) {
        return find(types, Set.of(TokenType.LPAREN), Set.of(TokenType.RPAREN), 0);
    }

    /**
     * Find the index of the first token type at top-level (parenDepth == 0), scanning from 'current position'.
     *
     * @param types     a list of types to look for any of them.
     * @param lookahead a number of token to skip at the beginning.
     * @return size if not found.
     */
    public int find(Set<TokenType> types, int lookahead) {
        return find(types, Set.of(TokenType.LPAREN), Set.of(TokenType.RPAREN), lookahead);
    }

    /**
     * Find the index of the first token type at top-level (parenDepth == 0), scanning from 'current position'.
     *
     * @param types     a list of types to look for any of them.
     * @param startSkip a token type to start skipping if met. By default, it is {@link TokenType#LPAREN}.
     * @param endSkip   a token type to stop skipping if met. By default, it is {@link TokenType#RPAREN}.
     * @param lookahead a number of token to skip at the beginning.
     * @return size if not found.
     */
    public int find(Set<TokenType> types, Set<TokenType> startSkip, Set<TokenType> endSkip, int lookahead) {
        int depth = 0;

        for (int i = pos + lookahead; i < tokens.size(); i++) {
            var t = tokens.get(i);

            if (startSkip.contains(t.type())) depth++;
            else if (depth > 0 && endSkip.contains(t.type())) depth = Math.max(0, depth - 1);
            else if (depth == 0 && types.contains(t.type())) {
                return i;
            }
        }
        return tokens.size();
    }
}
