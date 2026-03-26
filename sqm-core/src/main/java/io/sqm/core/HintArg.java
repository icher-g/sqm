package io.sqm.core;

import io.sqm.core.match.HintArgMatch;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Typed hint argument root.
 */
public sealed interface HintArg extends Node permits ExpressionHintArg, IdentifierHintArg, QualifiedNameHintArg {

    /**
     * Converts a convenience value into a typed hint argument.
     *
     * <p>Conversion rules:</p>
     * <ul>
     * <li>{@link HintArg}: returned unchanged</li>
     * <li>{@link Identifier}: converted to {@link IdentifierHintArg}</li>
     * <li>{@link QualifiedName}: converted to {@link QualifiedNameHintArg}</li>
     * <li>{@link Expression}: converted to {@link ExpressionHintArg}</li>
     * <li>{@link String}: treated as an identifier</li>
     * <li>any other value: wrapped as a literal expression</li>
     * </ul>
     *
     * @param value convenience value to convert
     * @return typed hint argument
     */
    static HintArg from(Object value) {
        Objects.requireNonNull(value, "value");
        return switch (value) {
            case HintArg hintArg -> hintArg;
            case Identifier identifier -> identifier(identifier);
            case QualifiedName qualifiedName -> qualifiedName(qualifiedName);
            case Expression expression -> expression(expression);
            case String identifier -> identifier(identifier);
            default -> expression(Expression.literal(value));
        };
    }

    /**
     * Converts convenience values into typed hint arguments.
     *
     * @param values convenience values to convert
     * @return ordered typed hint arguments
     */
    static List<HintArg> fromAll(Object... values) {
        Objects.requireNonNull(values, "values");
        return Arrays.stream(values)
            .map(HintArg::from)
            .toList();
    }

    /**
     * Creates an identifier-based hint argument.
     *
     * @param identifier identifier value
     * @return hint argument
     */
    static HintArg identifier(Identifier identifier) {
        return IdentifierHintArg.of(identifier);
    }

    /**
     * Creates an identifier-based hint argument.
     *
     * @param identifier identifier value
     * @return hint argument
     */
    static HintArg identifier(String identifier) {
        return identifier(Identifier.of(identifier));
    }

    /**
     * Creates a qualified-name-based hint argument.
     *
     * @param qualifiedName qualified name value
     * @return hint argument
     */
    static HintArg qualifiedName(QualifiedName qualifiedName) {
        return QualifiedNameHintArg.of(qualifiedName);
    }

    /**
     * Creates an expression-based hint argument.
     *
     * @param expression expression value
     * @return hint argument
     */
    static HintArg expression(Expression expression) {
        return ExpressionHintArg.of(expression);
    }

    /**
     * Creates a matcher for this hint argument.
     *
     * @param <R> result type
     * @return hint-argument matcher
     */
    default <R> HintArgMatch<R> matchHintArg() {
        return HintArgMatch.match(this);
    }
}