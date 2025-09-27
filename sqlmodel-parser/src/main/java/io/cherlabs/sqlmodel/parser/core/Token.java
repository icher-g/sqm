package io.cherlabs.sqlmodel.parser.core;

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
