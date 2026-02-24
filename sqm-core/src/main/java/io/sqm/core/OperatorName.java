package io.sqm.core;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a SQL operator name together with the notation used to express it.
 *
 * <p>This supports both bare operator tokens (for example {@code +}, {@code ->}, {@code @>})
 * and SQL {@code OPERATOR(...)} keyword syntax, optionally schema-qualified.</p>
 *
 * @param schemaName optional schema name used in {@code OPERATOR(schema.op)} syntax
 * @param symbol operator symbol token (not blank)
 * @param syntax notation used to express the operator
 */
public record OperatorName(QualifiedName schemaName, String symbol, Syntax syntax) implements Serializable {

    /**
     * Operator notation style.
     */
    public enum Syntax {
        /**
         * Bare operator token syntax (for example {@code +} or {@code ->}).
         */
        BARE,
        /**
         * {@code OPERATOR(...)} keyword syntax.
         */
        OPERATOR
    }

    /**
     * Creates an operator name.
     *
     * @param schemaName optional schema name used in {@code OPERATOR(...)} syntax
     * @param symbol operator symbol token
     * @param syntax notation used to express the operator
     */
    public OperatorName {
        Objects.requireNonNull(symbol, "symbol");
        Objects.requireNonNull(syntax, "syntax");
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
        if (schemaName != null && syntax != Syntax.OPERATOR) {
            throw new IllegalArgumentException("schemaName requires OPERATOR syntax");
        }
    }

    /**
     * Creates a bare operator token.
     *
     * @param symbol operator symbol token
     * @return operator name
     */
    public static OperatorName of(String symbol) {
        return new OperatorName(null, symbol, Syntax.BARE);
    }

    /**
     * Creates an {@code OPERATOR(symbol)} operator.
     *
     * @param symbol operator symbol token
     * @return operator name
     */
    public static OperatorName operator(String symbol) {
        return new OperatorName(null, symbol, Syntax.OPERATOR);
    }

    /**
     * Creates an {@code OPERATOR(schema.symbol)} operator.
     *
     * @param schemaName schema name
     * @param symbol operator symbol token
     * @return operator name
     */
    public static OperatorName operator(QualifiedName schemaName, String symbol) {
        return new OperatorName(Objects.requireNonNull(schemaName, "schemaName"), symbol, Syntax.OPERATOR);
    }

    /**
     * Indicates whether the operator uses {@code OPERATOR(...)} keyword syntax.
     *
     * @return {@code true} if {@code OPERATOR(...)} syntax is used
     */
    public boolean operatorKeywordSyntax() {
        return syntax == Syntax.OPERATOR;
    }

    /**
     * Indicates whether the operator has a schema-qualified name.
     *
     * @return {@code true} if a schema name is present
     */
    public boolean qualified() {
        return schemaName != null;
    }

    /**
     * Returns the historical textual representation used by compatibility APIs.
     *
     * @return operator text
     */
    public String text() {
        if (!operatorKeywordSyntax()) {
            return symbol;
        }
        if (schemaName == null) {
            return "OPERATOR(" + symbol + ")";
        }
        return "OPERATOR(" + String.join(".", schemaName.values()) + "." + symbol + ")";
    }
}
