package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Optional;

/**
 * Represents a SQL type name as it appears in SQL text.
 *
 * <p>This model is syntactic rather than semantic. It captures the textual
 * structure of a type name, while validation of dialect-specific support
 * (for example, array types or time zone clauses) is performed by renderers
 * or validators.</p>
 *
 * <p>A {@code TypeName} may be represented in exactly one of two forms:</p>
 * <ul>
 *   <li>As a qualified identifier sequence, such as {@code int}, {@code varchar},
 *       or {@code pg_catalog.int4}</li>
 *   <li>As a SQL keyword-based type, such as {@code DOUBLE PRECISION} or
 *       {@code CHARACTER VARYING}</li>
 * </ul>
 *
 * <p>Exactly one of {@link #qualifiedName()} or {@link #keyword()} must be present.</p>
 */
public non-sealed interface TypeName extends Node {

    /**
     * Creates a {@link TypeName} instance.
     *
     * <p>Exactly one of {@code qualifiedName} or {@code keyword} must be provided.</p>
     *
     * @param qualifiedName schema-qualified or unqualified identifier parts
     * @param keyword       SQL keyword-based type, or {@code null} if not applicable
     * @param modifiers     optional type modifiers, such as {@code (10)} or {@code (10,2)}
     * @param arrayDims     number of array dimensions
     * @param timeZoneSpec  time zone clause specification
     * @return a new {@link TypeName} instance
     */
    static TypeName of(List<String> qualifiedName, TypeKeyword keyword, List<Expression> modifiers, int arrayDims, TimeZoneSpec timeZoneSpec) {
        return new Impl(qualifiedName, Optional.ofNullable(keyword), modifiers, arrayDims, timeZoneSpec);
    }

    /**
     * Returns the schema-qualified or unqualified identifier parts of the type name.
     *
     * <p>For example:</p>
     * <ul>
     *   <li>{@code ["int"]}</li>
     *   <li>{@code ["pg_catalog", "int4"]}</li>
     * </ul>
     *
     * <p>This list is empty if and only if {@link #keyword()} is present.</p>
     *
     * @return identifier parts of the type name
     */
    List<String> qualifiedName();

    /**
     * Returns the SQL keyword-based type, if this type name is represented
     * using grammar-defined keywords.
     *
     * <p>Examples include {@code DOUBLE PRECISION} and
     * {@code CHARACTER VARYING}.</p>
     *
     * <p>This value is present if and only if {@link #qualifiedName()} is empty.</p>
     *
     * @return the keyword-based type, if applicable
     */
    Optional<TypeKeyword> keyword();

    /**
     * Returns the type modifiers associated with this type name.
     *
     * <p>Type modifiers correspond to the parenthesized arguments of a type,
     * such as {@code varchar(10)} or {@code numeric(10,2)}.</p>
     *
     * <p>The list is empty if no modifiers are specified.</p>
     *
     * @return modifier expressions in source order
     */
    List<Expression> modifiers();

    /**
     * Returns the number of PostgreSQL-style array dimensions.
     *
     * <p>A value of {@code 0} indicates a non-array type.
     * A value of {@code 1} represents {@code []}, {@code 2} represents
     * {@code [][]}, and so on.</p>
     *
     * <p>ANSI renderers are expected to reject values greater than zero.</p>
     *
     * @return number of array dimensions
     */
    int arrayDims();

    /**
     * Returns the time zone clause specification associated with this type name.
     *
     * <p>This is applicable primarily to {@code TIME} and {@code TIMESTAMP}
     * types in some SQL dialects.</p>
     *
     * <p>ANSI renderers are expected to reject values other than
     * {@link TimeZoneSpec#NONE}.</p>
     *
     * @return the time zone clause specification
     */
    TimeZoneSpec timeZoneSpec();

    /**
     * Accepts a {@link NodeVisitor}.
     *
     * @param visitor the visitor
     * @param <R>     visitor return type
     * @return the visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitTypeName(this);
    }

    /**
     * Returns a new {@link TypeName} with the given type modifiers applied.
     *
     * <p>This method does not mutate the current instance. The returned
     * instance preserves the type name, array dimensions, and time zone
     * specification, replacing only the modifiers.</p>
     *
     * @param modifiers modifier expressions
     * @return a new {@link TypeName} with the specified modifiers
     */
    default TypeName withModifiers(Expression... modifiers) {
        return new Impl(qualifiedName(), keyword(), List.of(modifiers), arrayDims(), timeZoneSpec());
    }

    /**
     * Returns a new {@link TypeName} representing a one-dimensional array type.
     *
     * <p>This is equivalent to calling {@link #array(int)} with {@code 1}.</p>
     *
     * @return a new {@link TypeName} with one array dimension
     */
    default TypeName array() {
        return new Impl(qualifiedName(), keyword(), modifiers(), 1, timeZoneSpec());
    }

    /**
     * Returns a new {@link TypeName} representing an array type with the
     * specified number of dimensions.
     *
     * <p>This method performs only basic validation of the dimension count.
     * Dialect-specific support is validated by renderers.</p>
     *
     * @param dims number of array dimensions; must be {@code >= 0}
     * @return a new {@link TypeName} with the specified number of array dimensions
     */
    default TypeName array(int dims) {
        return new Impl(qualifiedName(), keyword(), modifiers(), dims, timeZoneSpec());
    }

    /**
     * Returns a new {@link TypeName} with the {@code WITH TIME ZONE} clause applied.
     *
     * <p>If a time zone clause is already present, it is replaced.</p>
     *
     * @return a new {@link TypeName} with {@link TimeZoneSpec#WITH_TIME_ZONE}
     */
    default TypeName withTimeZone() {
        return new Impl(qualifiedName(), keyword(), modifiers(), arrayDims(), TimeZoneSpec.WITH_TIME_ZONE);
    }

    /**
     * Returns a new {@link TypeName} with the {@code WITHOUT TIME ZONE} clause applied.
     *
     * <p>If a time zone clause is already present, it is replaced.</p>
     *
     * @return a new {@link TypeName} with {@link TimeZoneSpec#WITHOUT_TIME_ZONE}
     */
    default TypeName withoutTimeZone() {
        return new Impl(qualifiedName(), keyword(), modifiers(), arrayDims(), TimeZoneSpec.WITHOUT_TIME_ZONE);
    }

    /**
     * Default immutable implementation of {@link TypeName}.
     *
     * <p>This implementation enforces the following invariants:</p>
     * <ul>
     *   <li>Exactly one of {@code keyword} or {@code qualifiedName} must be present</li>
     *   <li>{@code arrayDims} must be non-negative</li>
     *   <li>{@code timeZoneSpec} defaults to {@link TimeZoneSpec#NONE} if null</li>
     * </ul>
     *
     * @param qualifiedName schema-qualified or unqualified identifier parts
     * @param keyword       SQL keyword-based type, or {@code null} if not applicable
     * @param modifiers     optional type modifiers, such as {@code (10)} or {@code (10,2)}
     * @param arrayDims     number of array dimensions
     * @param timeZoneSpec  time zone clause specification
     */
    record Impl(List<String> qualifiedName, Optional<TypeKeyword> keyword, List<Expression> modifiers, int arrayDims, TimeZoneSpec timeZoneSpec) implements TypeName {
        /**
         * Creates a {@link TypeName.Impl} instance.
         *
         * @throws IllegalArgumentException if invariants are violated
         */
        public Impl {
            qualifiedName = qualifiedName == null ? List.of() : List.copyOf(qualifiedName);
            modifiers = modifiers == null ? List.of() : List.copyOf(modifiers);

            if (keyword.isPresent() && !qualifiedName.isEmpty()) {
                throw new IllegalArgumentException("TypeName cannot have both keyword and qualifiedName");
            }

            if (keyword.isEmpty() && qualifiedName.isEmpty()) {
                throw new IllegalArgumentException("TypeName must have either keyword or qualifiedName");
            }

            if (arrayDims < 0) {
                throw new IllegalArgumentException("arrayDims must be >= 0");
            }

            if (timeZoneSpec == null) {
                timeZoneSpec = TimeZoneSpec.NONE;
            }
        }
    }
}

