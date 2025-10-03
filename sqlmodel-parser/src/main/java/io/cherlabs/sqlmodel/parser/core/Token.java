package io.cherlabs.sqlmodel.parser.core;

/**
 * Represents a token.
 * @param type a token type.
 * @param lexeme an actual text.
 * @param pos a position in the tokens list.
 */
public record Token(TokenType type, String lexeme, int pos) {
    @Override
    public String toString() {
        return type + (lexeme != null ? "[" + lexeme + "]" : "") + "@" + pos;
    }

    public int start() {
        return pos;
    }

    public int end() {
        return pos + lexeme.length();
    }
}
