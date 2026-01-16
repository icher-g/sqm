package io.sqm.parser.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OperatorTokensTest {

    @Test
    void is_returnsTrueForMatchingOperator() {
        Token token = new Token(TokenType.OPERATOR, "=", 0);
        assertTrue(OperatorTokens.is(token, "="));
        assertFalse(OperatorTokens.is(token, "<>"));
    }

    @Test
    void is_returnsFalseForNonOperatorToken() {
        Token token = new Token(TokenType.SELECT, "SELECT", 0);
        assertFalse(OperatorTokens.is(token, "SELECT"));
    }

    @Test
    void isComparison_identifiesComparisonOperators() {
        assertTrue(OperatorTokens.isComparison(new Token(TokenType.OPERATOR, "=", 0)));
        assertTrue(OperatorTokens.isComparison(new Token(TokenType.OPERATOR, "<>", 0)));
        assertTrue(OperatorTokens.isComparison(new Token(TokenType.OPERATOR, "!=", 0)));
        assertTrue(OperatorTokens.isComparison(new Token(TokenType.OPERATOR, "<", 0)));
        assertTrue(OperatorTokens.isComparison(new Token(TokenType.OPERATOR, "<=", 0)));
        assertTrue(OperatorTokens.isComparison(new Token(TokenType.OPERATOR, ">", 0)));
        assertTrue(OperatorTokens.isComparison(new Token(TokenType.OPERATOR, ">=", 0)));
    }

    @Test
    void isComparison_returnsFalseForNonComparisonOperators() {
        assertFalse(OperatorTokens.isComparison(new Token(TokenType.OPERATOR, "+", 0)));
        assertFalse(OperatorTokens.isComparison(new Token(TokenType.OPERATOR, "-", 0)));
        assertFalse(OperatorTokens.isComparison(new Token(TokenType.OPERATOR, "*", 0)));
        assertFalse(OperatorTokens.isComparison(new Token(TokenType.OPERATOR, "/", 0)));
        assertFalse(OperatorTokens.isComparison(new Token(TokenType.OPERATOR, "||", 0)));
    }

    @Test
    void isComparison_returnsFalseForNonOperatorTokens() {
        assertFalse(OperatorTokens.isComparison(new Token(TokenType.SELECT, "SELECT", 0)));
        assertFalse(OperatorTokens.isComparison(new Token(TokenType.IDENT, "x", 0)));
    }

    @Test
    void isArithmetic_identifiesArithmeticOperators() {
        assertTrue(OperatorTokens.isArithmetic(new Token(TokenType.OPERATOR, "+", 0)));
        assertTrue(OperatorTokens.isArithmetic(new Token(TokenType.OPERATOR, "-", 0)));
        assertTrue(OperatorTokens.isArithmetic(new Token(TokenType.OPERATOR, "*", 0)));
        assertTrue(OperatorTokens.isArithmetic(new Token(TokenType.OPERATOR, "/", 0)));
        assertTrue(OperatorTokens.isArithmetic(new Token(TokenType.OPERATOR, "%", 0)));
    }

    @Test
    void isArithmetic_returnsFalseForNonArithmeticOperators() {
        assertFalse(OperatorTokens.isArithmetic(new Token(TokenType.OPERATOR, "=", 0)));
        assertFalse(OperatorTokens.isArithmetic(new Token(TokenType.OPERATOR, "<>", 0)));
        assertFalse(OperatorTokens.isArithmetic(new Token(TokenType.OPERATOR, "||", 0)));
    }

    @Test
    void isArithmetic_returnsFalseForNonOperatorTokens() {
        assertFalse(OperatorTokens.isArithmetic(new Token(TokenType.SELECT, "SELECT", 0)));
        assertFalse(OperatorTokens.isArithmetic(new Token(TokenType.NUMBER, "42", 0)));
    }

    @Test
    void isEq_identifiesEqualityOperator() {
        assertTrue(OperatorTokens.isEq(new Token(TokenType.OPERATOR, "=", 0)));
        assertFalse(OperatorTokens.isEq(new Token(TokenType.OPERATOR, "!=", 0)));
        assertFalse(OperatorTokens.isEq(new Token(TokenType.OPERATOR, "<>", 0)));
    }

    @Test
    void isNeqAngle_identifiesAngleNotEquals() {
        assertTrue(OperatorTokens.isNeqAngle(new Token(TokenType.OPERATOR, "<>", 0)));
        assertFalse(OperatorTokens.isNeqAngle(new Token(TokenType.OPERATOR, "!=", 0)));
        assertFalse(OperatorTokens.isNeqAngle(new Token(TokenType.OPERATOR, "=", 0)));
    }

    @Test
    void isNeqBang_identifiesBangNotEquals() {
        assertTrue(OperatorTokens.isNeqBang(new Token(TokenType.OPERATOR, "!=", 0)));
        assertFalse(OperatorTokens.isNeqBang(new Token(TokenType.OPERATOR, "<>", 0)));
        assertFalse(OperatorTokens.isNeqBang(new Token(TokenType.OPERATOR, "=", 0)));
    }

    @Test
    void isLt_identifiesLessThan() {
        assertTrue(OperatorTokens.isLt(new Token(TokenType.OPERATOR, "<", 0)));
        assertFalse(OperatorTokens.isLt(new Token(TokenType.OPERATOR, "<=", 0)));
        assertFalse(OperatorTokens.isLt(new Token(TokenType.OPERATOR, ">", 0)));
    }

    @Test
    void isLte_identifiesLessThanOrEqual() {
        assertTrue(OperatorTokens.isLte(new Token(TokenType.OPERATOR, "<=", 0)));
        assertFalse(OperatorTokens.isLte(new Token(TokenType.OPERATOR, "<", 0)));
        assertFalse(OperatorTokens.isLte(new Token(TokenType.OPERATOR, ">=", 0)));
    }

    @Test
    void isGt_identifiesGreaterThan() {
        assertTrue(OperatorTokens.isGt(new Token(TokenType.OPERATOR, ">", 0)));
        assertFalse(OperatorTokens.isGt(new Token(TokenType.OPERATOR, ">=", 0)));
        assertFalse(OperatorTokens.isGt(new Token(TokenType.OPERATOR, "<", 0)));
    }

    @Test
    void isGte_identifiesGreaterThanOrEqual() {
        assertTrue(OperatorTokens.isGte(new Token(TokenType.OPERATOR, ">=", 0)));
        assertFalse(OperatorTokens.isGte(new Token(TokenType.OPERATOR, ">", 0)));
        assertFalse(OperatorTokens.isGte(new Token(TokenType.OPERATOR, "<=", 0)));
    }

    @Test
    void isPlus_identifiesPlusOperator() {
        assertTrue(OperatorTokens.isPlus(new Token(TokenType.OPERATOR, "+", 0)));
        assertFalse(OperatorTokens.isPlus(new Token(TokenType.OPERATOR, "-", 0)));
        assertFalse(OperatorTokens.isPlus(new Token(TokenType.OPERATOR, "*", 0)));
    }

    @Test
    void isMinus_identifiesMinusOperator() {
        assertTrue(OperatorTokens.isMinus(new Token(TokenType.OPERATOR, "-", 0)));
        assertFalse(OperatorTokens.isMinus(new Token(TokenType.OPERATOR, "+", 0)));
        assertFalse(OperatorTokens.isMinus(new Token(TokenType.OPERATOR, "->", 0)));
    }

    @Test
    void isStar_identifiesStarOperator() {
        assertTrue(OperatorTokens.isStar(new Token(TokenType.OPERATOR, "*", 0)));
        assertFalse(OperatorTokens.isStar(new Token(TokenType.OPERATOR, "/", 0)));
        assertFalse(OperatorTokens.isStar(new Token(TokenType.OPERATOR, "+", 0)));
    }

    @Test
    void isSlash_identifiesSlashOperator() {
        assertTrue(OperatorTokens.isSlash(new Token(TokenType.OPERATOR, "/", 0)));
        assertFalse(OperatorTokens.isSlash(new Token(TokenType.OPERATOR, "*", 0)));
        assertFalse(OperatorTokens.isSlash(new Token(TokenType.OPERATOR, "%", 0)));
    }

    @Test
    void isPercent_identifiesPercentOperator() {
        assertTrue(OperatorTokens.isPercent(new Token(TokenType.OPERATOR, "%", 0)));
        assertFalse(OperatorTokens.isPercent(new Token(TokenType.OPERATOR, "/", 0)));
        assertFalse(OperatorTokens.isPercent(new Token(TokenType.OPERATOR, "*", 0)));
    }

    @Test
    void allMethods_handleNullLexemeGracefully() {
        Token token = new Token(TokenType.IDENT, "test", 0);
        assertFalse(OperatorTokens.is(token, "="));
        assertFalse(OperatorTokens.isComparison(token));
        assertFalse(OperatorTokens.isArithmetic(token));
    }

    @Test
    void multiCharOperators_areHandledCorrectly() {
        assertTrue(OperatorTokens.is(new Token(TokenType.OPERATOR, "->", 0), "->"));
        assertTrue(OperatorTokens.is(new Token(TokenType.OPERATOR, "->>", 0), "->>"));
        assertTrue(OperatorTokens.is(new Token(TokenType.OPERATOR, "||", 0), "||"));
        assertTrue(OperatorTokens.is(new Token(TokenType.OPERATOR, "&&", 0), "&&"));
    }
}
