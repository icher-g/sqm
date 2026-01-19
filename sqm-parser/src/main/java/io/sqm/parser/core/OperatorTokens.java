package io.sqm.parser.core;

import java.util.Set;

/**
 * Helper methods for working with {@link TokenType#OPERATOR} tokens.
 * <p>
 * The lexer emits many symbolic operators (for example {@code =}, {@code <=}, {@code +}, {@code ->>})
 * as {@code OPERATOR} tokens. The concrete operator text is stored in the token text/lexeme.
 */
public final class OperatorTokens {

    private static final Set<String> COMPARISON = Set.of("=", "<>", "!=", "<", "<=", ">", ">=");
    private static final Set<String> ARITHMETIC = Set.of("+", "-", "*", "/", "%");
    private static final Set<String> REGEX = Set.of("~", "~*", "!~", "!~*");

    private OperatorTokens() {
    }

    /**
     * Returns {@code true} if the given token is an {@code OPERATOR} with the specified operator text.
     *
     * @param t  token to test
     * @param op operator text, for example {@code "="} or {@code "->>"}
     * @return {@code true} if token is {@code OPERATOR(op)}
     */
    public static boolean is(Token t, String op) {
        return t.type() == TokenType.OPERATOR && op.equals(t.lexeme());
    }

    /**
     * Returns {@code true} if the given token represents one of the comparison operators:
     * {@code =}, {@code <>}, {@code !=}, {@code <}, {@code <=}, {@code >}, {@code >=}.
     *
     * @param t token to test
     * @return {@code true} if token is a comparison operator
     */
    public static boolean isComparison(Token t) {
        return t.type() == TokenType.OPERATOR && COMPARISON.contains(t.lexeme());
    }

    /**
     * Returns {@code true} if the given token represents one of the arithmetic operators:
     * {@code +}, {@code -}, {@code *}, {@code /}, {@code %}.
     *
     * @param t token to test
     * @return {@code true} if token is an arithmetic operator
     */
    public static boolean isArithmetic(Token t) {
        return t.type() == TokenType.OPERATOR && ARITHMETIC.contains(t.lexeme());
    }

    /**
     * Returns {@code true} if the given token represents one of the regex operators:
     * {@code ~}, {@code ~*}, {@code !~}, {@code !~*}.
     *
     * @param t token to test
     * @return {@code true} if token is a regex operator
     */
    public static boolean isRegex(Token t) {
        return t.type() == TokenType.OPERATOR && REGEX.contains(t.lexeme());
    }

    /**
     * @return {@code true} if token is {@code OPERATOR("=")}
     */
    public static boolean isEq(Token t) {
        return is(t, "=");
    }

    /**
     * @return {@code true} if token is {@code OPERATOR("<>")}
     */
    public static boolean isNeqAngle(Token t) {
        return is(t, "<>");
    }

    /**
     * @return {@code true} if token is {@code OPERATOR("!=")}
     */
    public static boolean isNeqBang(Token t) {
        return is(t, "!=");
    }

    /**
     * @return {@code true} if token is {@code OPERATOR("<")}
     */
    public static boolean isLt(Token t) {
        return is(t, "<");
    }

    /**
     * @return {@code true} if token is {@code OPERATOR("<=")}
     */
    public static boolean isLte(Token t) {
        return is(t, "<=");
    }

    /**
     * @return {@code true} if token is {@code OPERATOR(">")}
     */
    public static boolean isGt(Token t) {
        return is(t, ">");
    }

    /**
     * @return {@code true} if token is {@code OPERATOR(">=")}
     */
    public static boolean isGte(Token t) {
        return is(t, ">=");
    }

    /**
     * @return {@code true} if token is {@code OPERATOR("+")}
     */
    public static boolean isPlus(Token t) {
        return is(t, "+");
    }

    /**
     * @return {@code true} if token is {@code OPERATOR("-")}
     */
    public static boolean isMinus(Token t) {
        return is(t, "-");
    }

    /**
     * @return {@code true} if token is {@code OPERATOR("*")}
     */
    public static boolean isStar(Token t) {
        return is(t, "*");
    }

    /**
     * @return {@code true} if token is {@code OPERATOR("/")}
     */
    public static boolean isSlash(Token t) {
        return is(t, "/");
    }

    /**
     * @return {@code true} if token is {@code OPERATOR("%")}
     */
    public static boolean isPercent(Token t) {
        return is(t, "%");
    }
}