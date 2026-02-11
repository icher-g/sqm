package io.sqm.validate.api;

/**
 * Generic contract for model validation.
 *
 * @param <T> input model type.
 * @param <R> validation result type.
 */
public interface Validator<T, R> {
    /**
     * Validates provided input.
     *
     * @param target input to validate.
     * @return validation result.
     */
    R validate(T target);
}

