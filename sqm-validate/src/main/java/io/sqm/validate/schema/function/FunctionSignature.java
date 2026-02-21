package io.sqm.validate.schema.function;

import io.sqm.catalog.model.CatalogType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Function signature descriptor used for semantic validation.
 *
 * @param minArity   minimum number of accepted arguments.
 * @param maxArity   maximum number of accepted arguments.
 * @param argKinds   expected argument kinds by position; when fewer than actual arity, the last kind is reused.
 * @param returnType optional function return type for expression type inference.
 * @param aggregate  whether function is treated as aggregate in non-window context.
 */
public record FunctionSignature(
    int minArity,
    int maxArity,
    List<FunctionArgKind> argKinds,
    Optional<CatalogType> returnType,
    boolean aggregate
) {
    /**
     * Creates a function signature without explicit return type metadata.
     *
     * @param minArity minimum accepted arity.
     * @param maxArity maximum accepted arity.
     * @param argKinds expected argument kinds by position.
     */
    public FunctionSignature(int minArity, int maxArity, List<FunctionArgKind> argKinds) {
        this(minArity, maxArity, argKinds, Optional.empty(), false);
    }

    /**
     * Creates a function signature without explicit return type metadata.
     *
     * @param minArity  minimum accepted arity.
     * @param maxArity  maximum accepted arity.
     * @param argKinds  expected argument kinds by position.
     * @param aggregate whether function is aggregate.
     */
    public FunctionSignature(int minArity, int maxArity, List<FunctionArgKind> argKinds, boolean aggregate) {
        this(minArity, maxArity, argKinds, Optional.empty(), aggregate);
    }

    /**
     * Creates a function signature.
     *
     * @param minArity   minimum accepted arity.
     * @param maxArity   maximum accepted arity.
     * @param argKinds   expected argument kinds by position.
     * @param returnType optional return type metadata.
     */
    public FunctionSignature(int minArity, int maxArity, List<FunctionArgKind> argKinds, CatalogType returnType) {
        this(minArity, maxArity, argKinds, Optional.of(returnType), false);
    }

    /**
     * Creates a function signature.
     *
     * @param minArity   minimum accepted arity.
     * @param maxArity   maximum accepted arity.
     * @param argKinds   expected argument kinds by position.
     * @param returnType optional return type metadata.
     * @param aggregate  whether function is aggregate.
     */
    public FunctionSignature {
        if (minArity < 0) {
            throw new IllegalArgumentException("minArity must be >= 0");
        }
        if (maxArity < minArity) {
            throw new IllegalArgumentException("maxArity must be >= minArity");
        }
        Objects.requireNonNull(argKinds, "argKinds");
        Objects.requireNonNull(returnType, "returnType");
        argKinds = List.copyOf(argKinds);
    }

    /**
     * Creates a signature instance.
     *
     * @param minArity minimum accepted arity.
     * @param maxArity maximum accepted arity.
     * @param argKinds expected argument kinds by position.
     * @return function signature.
     */
    public static FunctionSignature of(int minArity, int maxArity, FunctionArgKind... argKinds) {
        return new FunctionSignature(minArity, maxArity, List.of(argKinds));
    }

    /**
     * Creates a signature instance with explicit return type metadata.
     *
     * @param minArity   minimum accepted arity.
     * @param maxArity   maximum accepted arity.
     * @param returnType inferred function return type.
     * @param argKinds   expected argument kinds by position.
     * @return function signature.
     */
    public static FunctionSignature of(int minArity, int maxArity, CatalogType returnType, FunctionArgKind... argKinds) {
        return new FunctionSignature(minArity, maxArity, List.of(argKinds), returnType);
    }

    /**
     * Creates an aggregate signature instance.
     *
     * @param minArity minimum accepted arity.
     * @param maxArity maximum accepted arity.
     * @param argKinds expected argument kinds by position.
     * @return aggregate function signature.
     */
    public static FunctionSignature ofAggregate(int minArity, int maxArity, FunctionArgKind... argKinds) {
        return new FunctionSignature(minArity, maxArity, List.of(argKinds), true);
    }

    /**
     * Creates an aggregate signature instance with explicit return type metadata.
     *
     * @param minArity   minimum accepted arity.
     * @param maxArity   maximum accepted arity.
     * @param returnType inferred function return type.
     * @param argKinds   expected argument kinds by position.
     * @return aggregate function signature.
     */
    public static FunctionSignature ofAggregate(int minArity, int maxArity, CatalogType returnType, FunctionArgKind... argKinds) {
        return new FunctionSignature(minArity, maxArity, List.of(argKinds), Optional.of(returnType), true);
    }
}

