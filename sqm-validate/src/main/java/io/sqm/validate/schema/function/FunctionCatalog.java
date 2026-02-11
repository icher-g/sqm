package io.sqm.validate.schema.function;

import java.util.Optional;

/**
 * Resolves function signatures by function name.
 */
@FunctionalInterface
public interface FunctionCatalog {
    /**
     * Resolves signature for function name.
     *
     * @param functionName function name.
     * @return resolved signature when known.
     */
    Optional<FunctionSignature> resolve(String functionName);
}

