package io.sqm.core;

/**
 * Represents an arithmetic expression of the <em>additive</em> precedence level.
 * <p>
 * Additive expressions correspond to the SQL operators {@code +} and {@code -},
 * which have lower precedence than multiplicative operators. This ensures correct
 * grouping of expressions such as {@code a - b - c} into left-associative form
 * {@code (a - b) - c}, and correct interaction with multiplication, e.g.
 * {@code a + b * 2} being parsed as {@code a + (b * 2)}.
 * </p>
 *
 * <p>This interface is a specialization of {@link BinaryArithmeticExpr}, where both
 * operands are arbitrary {@link Expression} nodes representing scalar numeric
 * expressions.</p>
 *
 * <p>The permitted subtypes correspond to the additive SQL operators:</p>
 * <ul>
 *     <li>{@link AddArithmeticExpr} for addition ({@code +})</li>
 *     <li>{@link SubArithmeticExpr} for subtraction ({@code -})</li>
 * </ul>
 *
 * @see MultiplicativeArithmeticExpr
 * @see BinaryArithmeticExpr
 */
public sealed interface AdditiveArithmeticExpr
    extends BinaryArithmeticExpr
    permits AddArithmeticExpr, SubArithmeticExpr {
}

