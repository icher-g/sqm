package io.sqm.core;

/**
 * Represents an arithmetic expression of the <em>multiplicative</em> precedence level.
 * <p>
 * Multiplicative expressions correspond to the SQL operators with higher precedence:
 * {@code *}, {@code /}, and {@code %} (or {@code MOD} depending on dialect).
 * These expressions bind more tightly than additive expressions, ensuring that
 * expressions such as {@code a + b * 2} are parsed as {@code a + (b * 2)}.
 * </p>
 *
 * <p>This interface is a specialization of {@link BinaryArithmeticExpr}, where both
 * the left-hand side and right-hand side operands are arbitrary {@link Expression}
 * nodes that evaluate to numeric scalar values.</p>
 *
 * <p>The permitted subtypes reflect the multiplicative SQL operators supported by
 * the SQM model:</p>
 * <ul>
 *     <li>{@link MulArithmeticExpr} for multiplication ({@code *})</li>
 *     <li>{@link DivArithmeticExpr} for division ({@code /})</li>
 *     <li>{@link ModArithmeticExpr} for modulo ({@code %} or {@code MOD})</li>
 * </ul>
 *
 * @see AdditiveArithmeticExpr
 * @see BinaryArithmeticExpr
 */
public sealed interface MultiplicativeArithmeticExpr
    extends BinaryArithmeticExpr
    permits DivArithmeticExpr, ModArithmeticExpr, MulArithmeticExpr {
}

