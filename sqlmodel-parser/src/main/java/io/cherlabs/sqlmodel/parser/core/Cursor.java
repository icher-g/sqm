package io.cherlabs.sqlmodel.parser.core;

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

    public Cursor(List<Token> tokens) {
        this(tokens, 0);
    }

    public Cursor(List<Token> tokens, int start) {
        var copy = new ArrayList<>(tokens);
        if (copy.isEmpty() || copy.get(copy.size() - 1).type() != TokenType.EOF) {
            int pos = copy.isEmpty() ? 0 : copy.get(copy.size() - 1).pos();
            copy.add(new Token(TokenType.EOF, "", pos));
        }
        this.tokens = copy;
        this.pos = start;
    }

    public int size() {
        return tokens.size();
    }

    public boolean hasNext() {
        return pos < tokens.size();
    }

    public boolean hasNext(int lookahead) {
        return pos + lookahead < tokens.size();
    }

    public Token peek() {
        if (!hasNext()) throw new NoSuchElementException("No more tokens");
        return tokens.get(pos);
    }

    public Token peek(int lookahead) {
        if (!hasNext(lookahead)) throw new NoSuchElementException("No more tokens");
        return tokens.get(pos + lookahead);
    }

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

    public int pos() {
        return pos;
    }

    public void setPos(int pos) {
        if (pos < 0 || pos > tokens.size() - 1)
            throw new IndexOutOfBoundsException("The provided pos is out of bounds: " + pos);
        this.pos = pos;
    }

    public boolean isEof() {
        return match(TokenType.EOF);
    }

    public boolean match(TokenType type) {
        return peek().type() == type;
    }

    public boolean match(TokenType type, int lookahead) {
        return peek(lookahead).type() == type;
    }

    public boolean matchAny(TokenType... types) {
        var type = peek().type();
        return Arrays.stream(types).anyMatch(tt -> type == tt);
    }

    public boolean matchAny(int lookahead, TokenType... types) {
        var type = peek(lookahead).type();
        return Arrays.stream(types).anyMatch(tt -> type == tt);
    }

    public boolean consumeIf(TokenType type) {
        if (match(type)) {
            pos++;
            return true;
        }
        return false;
    }

    /**
     * Expect token; throw otherwise.
     */
    public Token expect(TokenType... types) {
        return expect(
                tts -> "Expected token(s) " + Arrays.stream(types).map(Enum::toString).collect(Collectors.joining(",")) + " at position " + pos,
                types);
    }

    public Token expect(String message, TokenType... types) {
        return expect(tts -> message, types);
    }

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
        var set = Set.of(types);
        int depth = 0;

        for (int i = pos; i < tokens.size(); i++) {
            var t = tokens.get(i);

            // track parentheses only – our lexer already isolated string/quoted identifiers
            if (t.type() == TokenType.LPAREN) depth++;
            else if (t.type() == TokenType.RPAREN) depth = Math.max(0, depth - 1);

            // a THEN that is not nested ends the filter slice
            if (depth == 0 && set.contains(t.type())) {
                return i;
            }
        }
        return -1;
    }

    public Cursor sliceUntil(int end) {
        if (end < pos || end > tokens.size()) {
            throw new IndexOutOfBoundsException("Invalid slice end: " + end);
        }
        return new Cursor(tokens.subList(pos, end));
    }

    public Cursor sliceUntil(TokenType type) {
        int end = find(type);
        return sliceUntil(end);
    }
}
