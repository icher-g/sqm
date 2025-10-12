package io.cherlabs.sqm.parser.core;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cursor for token list that maintains current position.
 * All parsers share the same cursor instance to advance naturally.
 */
public final class Cursor {
    private final List<Token> tokens;
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
     * @param tokens a list of tokens.
     * @param start  a start point.
     */
    public Cursor(List<Token> tokens, int start) {
        var copy = new ArrayList<>(tokens);
        if (copy.isEmpty() || copy.get(copy.size() - 1).type() != TokenType.EOF) {
            int pos = copy.isEmpty() ? 0 : copy.get(copy.size() - 1).pos();
            copy.add(new Token(TokenType.EOF, "", pos));
        }
        this.tokens = copy;
        this.pos = start;
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
     * Indicates if there is a next token to handle.
     *
     * @return True if there is a next token available or False otherwise.
     */
    public boolean hasNext() {
        return pos < tokens.size();
    }

    /**
     * Indicates if there is a next lookahead token to handle.
     *
     * @param lookahead a number of tokens to lookahead.
     * @return True if there is a next lookahead token or False otherwise.
     */
    public boolean hasNext(int lookahead) {
        return pos + lookahead < tokens.size();
    }

    /**
     * Gets a current token.
     *
     * @return a token.
     */
    public Token peek() {
        if (!hasNext()) throw new NoSuchElementException("No more tokens");
        return tokens.get(pos);
    }

    /**
     * Gets a lookahead token from current position.
     *
     * @param lookahead a number of tokens to lookahead.
     * @return a token.
     */
    public Token peek(int lookahead) {
        if (!hasNext(lookahead)) throw new NoSuchElementException("No more tokens");
        return tokens.get(pos + lookahead);
    }

    /**
     * Gets a previous token.
     *
     * @return a token.
     */
    public Token prev() {
        return tokens.get(pos - 1);
    }

    /***
     * Returns current token and advances the cursor.
     * @return current Token
     */
    public Token advance() {
        if (!hasNext()) throw new NoSuchElementException("No more tokens");
        return tokens.get(pos++);
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
     * Expect token; throw otherwise.
     *
     * @param types a list of types to expect.
     */
    public Token expect(TokenType... types) {
        return expect(
                tts -> "Expected token(s) " + Arrays.stream(types).map(Enum::toString).collect(Collectors.joining(",")) + " at position " + pos,
                types);
    }

    /**
     * Expect token; throw otherwise.
     *
     * @param message an error message to throw in case of the error.
     * @param types   a list of types to expect.
     */
    public Token expect(String message, TokenType... types) {
        return expect(tts -> message, types);
    }

    /**
     * Expect token; throw otherwise.
     *
     * @param funcMessage a function to produce an error message in case of the error.
     * @param types       a list of types to expect.
     */
    public Token expect(Function<TokenType[], String> funcMessage, TokenType... types) {
        Token t = advance();
        var set = Set.of(types);
        if (!set.contains(t.type()))
            throw new ParserException(funcMessage.apply(types), t.pos());
        return t;
    }

    /**
     * Find the index of the first token type at top-level (parenDepth == 0),
     * scanning from 'current position'. Returns -1 if not found.
     */
    public int find(TokenType... types) {
        return find(Set.of(types));
    }

    /**
     * Find the index of the first token type at top-level (parenDepth == 0),
     * scanning from 'current position'. Returns size if not found.
     */
    public int find(Set<TokenType> types) {
        return find(types, 0);
    }

    /**
     * Find the index of the first token type at top-level (parenDepth == 0),
     * scanning from 'current position'. Returns size if not found.
     */
    public int find(Set<TokenType> types, int skip) {
        int depth = 0;

        for (int i = pos + skip; i < tokens.size(); i++) {
            var t = tokens.get(i);

            // track parentheses only – our lexer already isolated string/quoted identifiers
            if (t.type() == TokenType.LPAREN) depth++;
            else if (t.type() == TokenType.RPAREN) depth = Math.max(0, depth - 1);

            // a THEN that is not nested ends the filter slice
            if (depth == 0 && types.contains(t.type())) {
                return i;
            }
        }
        return tokens.size();
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
        var cur = new Cursor(tokens.subList(pos, end));
        pos = end;
        return cur;
    }
}
