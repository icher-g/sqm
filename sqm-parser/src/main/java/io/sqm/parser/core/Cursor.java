package io.sqm.parser.core;

import io.sqm.parser.spi.IdentifierQuoting;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;

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
        if (copy.isEmpty() || copy.getLast().type() != TokenType.EOF) {
            int pos = copy.isEmpty() ? 0 : copy.getLast().pos();
            copy.add(new Token(TokenType.EOF, "", pos));
        }
        this.tokens = copy;
        this.basePos = basePos;
        this.pos = 0;
    }

    /**
     * Creates a new instance of {@link Cursor} from the provided specification.
     *
     * @param spec a specification.
        * @param identifierQuoting identifier quoting rules used during lexing
     * @return a new instance of {@link Cursor}.
     */
    public static Cursor of(String spec, IdentifierQuoting identifierQuoting) {
        var ts = Lexer.lexAll(spec, identifierQuoting);
        return new Cursor(ts);
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
     * Gets the current cursor position.
     *
     * @return a current cursor position.
     */
    public int mark() {
        return pos;
    }

    /**
     * Restores current position of the cursor.
     *
     * @param mark a position to restore to.
     */
    public void restore(int mark) {
        this.pos = mark;
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
     * Validates if current token type and lexeme are the same as the specified.
     *
     * @param type   the type to match.
     * @param lexeme the lexeme to match.
     * @return True if the current token type matches the provided one.
     */
    public boolean match(TokenType type, String lexeme) {
        var t = peek();
        return t.type() == type && t.lexeme().equalsIgnoreCase(lexeme);
    }

    /**
     * Validates if current token type is the same as the provided one.
     *
     * @param func the custom function to match.
     * @return True if the current token type matches the provided one.
     */
    public boolean match(Function<Token, Boolean> func) {
        return func.apply(peek());
    }

    /**
     * Validates if current token type with respect to a lookahead position is the same as the provided one.
     *
     * @param type      the type to match.
     * @param lookahead the lookahead position.
     * @return True if the token type matches the provided one.
     */
    public boolean match(TokenType type, int lookahead) {
        if (lookahead >= tokens.size()) {
            return false;
        }
        return peek(lookahead).type() == type;
    }

    /**
     * Validates if current token type and lexeme with respect to a lookahead position are the same as specified.
     *
     * @param type      the type to match.
     * @param lexeme    the lexeme to match.
     * @param lookahead the lookahead position.
     * @return True if the token type matches the provided one.
     */
    public boolean match(TokenType type, String lexeme, int lookahead) {
        if (lookahead >= tokens.size()) {
            return false;
        }
        var t = peek(lookahead);
        return t.type() == type && t.lexeme().equalsIgnoreCase(lexeme);
    }

    /**
     * Validates if current token type with respect to a lookahead position is the same as the provided one.
     *
     * @param func      the custom function to match.
     * @param lookahead the lookahead position.
     * @return True if the token type matches the provided one.
     */
    public boolean match(Function<Token, Boolean> func, int lookahead) {
        if (lookahead >= tokens.size()) {
            return false;
        }
        return func.apply(peek(lookahead));
    }

    /**
     * Validates if current token type matches on of the provided token types.
     *
     * @param types the types to match.
     * @return True if one of the provided token types matches the current.
     */
    public boolean matchAny(TokenType... types) {
        return matchAny(Set.of(types), 0);
    }

    /**
     * Validates if current token type matches on of the provided token types.
     *
     * @param types the types to match.
     * @return True if one of the provided token types matches the current.
     */
    public boolean matchAny(Set<TokenType> types) {
        return matchAny(types, 0);
    }

    /**
     * Validates if current token type with respect to a lookahead position matches one of the provided token types.
     *
     * @param lookahead the lookahead position.
     * @param types     the types to match.
     * @return True if one of the provided token types matches the current + lookahead.
     */
    public boolean matchAny(int lookahead, TokenType... types) {
        return matchAny(Set.of(types), lookahead);
    }

    /**
     * Validates if current token type with respect to a lookahead position matches one of the provided token types.
     *
     * @param types     the types to match.
     * @param lookahead the lookahead position.
     * @return True if one of the provided token types matches the current + lookahead.
     */
    public boolean matchAny(Set<TokenType> types, int lookahead) {
        if (lookahead >= tokens.size()) {
            return false;
        }
        var type = peek(lookahead).type();
        return types.contains(type);
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
     * Advances the cursor if the match is found.
     *
     * @param func the custom function to match.
     * @return True if the token was consumed or False otherwise.
     */
    public boolean consumeIf(Function<Token, Boolean> func) {
        if (func.apply(peek())) {
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
     * @return consumed token
     */
    public Token expect(String message, TokenType... types) {
        return expect(message, Set.of(types));
    }

    /**
     * Expect token; throw otherwise. The cursor is advanced in any case.
     *
     * @param message an error message to throw if the token is not matched.
     * @param types   a list of types to expect.
     * @return consumed token
     */
    public Token expect(String message, Set<TokenType> types) {
        Token token = advance();
        if (!types.contains(token.type())) {
            throw new ParserException(message, token.pos());
        }
        return token;
    }

    /**
     * Expect token; throw otherwise. The cursor is advanced in any case.
     *
     * @param message an error message to throw if the token is not matched.
     * @param func    a custom validation function.
     * @return consumed token
     */
    public Token expect(String message, Function<Token, Boolean> func) {
        Token token = advance();
        if (!func.apply(token)) {
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
     * @param types     a list of types to look for any of them.
     * @param lookahead a number of token to skip at the beginning.
     * @return size if not found.
     */
    public int find(int lookahead, TokenType... types) {
        return find(Set.of(types), lookahead);
    }

    /**
     * Find the index of the first token type at top-level (parenDepth == 0), scanning from 'current position'.
     *
     * @param types a list of types to look for any of them.
     * @return size if not found.
     */
    public int find(Set<TokenType> types) {
        return find(types, 0);
    }

    /**
     * Find the index of the first token type at top-level (parenDepth == 0), scanning from 'current position'.
     *
     * @param types     a list of types to look for any of them.
     * @param lookahead a number of token to skip at the beginning.
     * @return size if not found.
     */
    public int find(Set<TokenType> types, int lookahead) {
        return find(types, Set.of(TokenType.LPAREN), Set.of(TokenType.RPAREN), Set.of(TokenType.EOF), lookahead);
    }

    /**
     * Find the index of the first token type at top-level (parenDepth == 0), scanning from 'current position'.
     *
     * @param types      a list of types to look for any of them.
     * @param stopLookup a token type(s) to stop the lookup if met. By default, it is {@link TokenType#EOF}.
     * @return size if not found.
     */
    public int find(Set<TokenType> types, Set<TokenType> stopLookup) {
        return find(types, stopLookup, 0);
    }

    /**
     * Find the index of the first token type at top-level (parenDepth == 0), scanning from 'current position'.
     *
     * @param types      a list of types to look for any of them.
     * @param stopLookup a token type(s) to stop the lookup if met. By default, it is {@link TokenType#EOF}.
     * @param lookahead  a number of token to skip at the beginning.
     * @return size if not found.
     */
    public int find(Set<TokenType> types, Set<TokenType> stopLookup, int lookahead) {
        return find(types, Set.of(TokenType.LPAREN), Set.of(TokenType.RPAREN), stopLookup, lookahead);
    }

    /**
     * Find the index of the first token type at top-level (parenDepth == 0), scanning from 'current position'.
     *
     * @param types      a list of types to look for any of them.
     * @param startSkip  a token type(s) to start skipping if met. By default, it is {@link TokenType#LPAREN}.
     * @param endSkip    a token type(s) to stop skipping if met. By default, it is {@link TokenType#RPAREN}.
     * @param stopLookup a token type(s) to stop the lookup if met. By default, it is {@link TokenType#EOF}.
     * @param lookahead  a number of token to skip at the beginning.
     * @return size if not found.
     */
    public int find(Set<TokenType> types, Set<TokenType> startSkip, Set<TokenType> endSkip, Set<TokenType> stopLookup, int lookahead) {
        return find(t -> types.contains(t.type()), startSkip, endSkip, stopLookup, lookahead);
    }

    /**
     * Find the index of the first token type at top-level (parenDepth == 0), scanning from 'current position'.
     *
     * @param lookupFunc a custom lookup function.
     * @return size if not found.
     */
    public int find(Function<Token, Boolean> lookupFunc) {
        return find(lookupFunc, 0);
    }

    /**
     * Find the index of the first token type at top-level (parenDepth == 0), scanning from 'current position'.
     *
     * @param lookupFunc a custom lookup function.
     * @param lookahead  a number of token to skip at the beginning.
     * @return size if not found.
     */
    public int find(Function<Token, Boolean> lookupFunc, int lookahead) {
        return find(lookupFunc, Set.of(TokenType.LPAREN), Set.of(TokenType.RPAREN), Set.of(TokenType.EOF), lookahead);
    }

    /**
     * Find the index of the first token type at top-level (parenDepth == 0), scanning from 'current position'.
     *
     * @param lookupFunc a custom lookup function.
     * @param stopLookup a token type(s) to stop the lookup if met. By default, it is {@link TokenType#EOF}.
     * @return size if not found.
     */
    public int find(Function<Token, Boolean> lookupFunc, Set<TokenType> stopLookup) {
        return find(lookupFunc, stopLookup, 0);
    }

    /**
     * Find the index of the first token type at top-level (parenDepth == 0), scanning from 'current position'.
     *
     * @param lookupFunc a custom lookup function.
     * @param stopLookup a token type(s) to stop the lookup if met. By default, it is {@link TokenType#EOF}.
     * @param lookahead  a number of token to skip at the beginning.
     * @return size if not found.
     */
    public int find(Function<Token, Boolean> lookupFunc, Set<TokenType> stopLookup, int lookahead) {
        return find(lookupFunc, Set.of(TokenType.LPAREN), Set.of(TokenType.RPAREN), stopLookup, lookahead);
    }

    /**
     * Find the index of the first token type at top-level (parenDepth == 0), scanning from 'current position'.
     *
     * @param lookupFunc a custom lookup function.
     * @param startSkip  a token type(s) to start skipping if met. By default, it is {@link TokenType#LPAREN}.
     * @param endSkip    a token type(s) to stop skipping if met. By default, it is {@link TokenType#RPAREN}.
     * @param stopLookup a token type(s) to stop the lookup if met. By default, it is {@link TokenType#EOF}.
     * @param lookahead  a number of token to skip at the beginning.
     * @return size if not found.
     */
    public int find(Function<Token, Boolean> lookupFunc, Set<TokenType> startSkip, Set<TokenType> endSkip, Set<TokenType> stopLookup, int lookahead) {
        int depth = 0;

        for (int i = pos + lookahead; i < tokens.size(); i++) {
            var t = tokens.get(i);

            if (stopLookup.contains(t.type())) {
                break;
            }

            if (startSkip.contains(t.type())) {
                depth++;
                continue;
            }

            if (depth > 0 && endSkip.contains(t.type())) {
                depth = Math.max(0, depth - 1);
                continue;
            }

            if (depth == 0 && lookupFunc.apply(t)) {
                return i;
            }
        }
        return tokens.size();
    }
}
