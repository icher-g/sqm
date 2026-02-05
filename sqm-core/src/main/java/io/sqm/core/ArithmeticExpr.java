package io.sqm.core;

/**
 * Represents any arithmetic expression, such as addition, subtraction,
 * multiplication, division, modulo, or unary negation.
 *
 * <p>This is the common parent for all arithmetic-related expression types.
 * Implementations include both binary operations (e.g. {@code a + b}) and
 * unary operations (e.g. {@code -expr}).</p>
 */
public sealed interface ArithmeticExpr extends Expression
    permits BinaryArithmeticExpr, NegativeArithmeticExpr, PowerArithmeticExpr {
}

