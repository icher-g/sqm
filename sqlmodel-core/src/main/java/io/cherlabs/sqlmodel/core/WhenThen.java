package io.cherlabs.sqlmodel.core;

/**
 * Represents a WHEN...THEN statement used in a CASE.
 *
 * @param when a when statement
 * @param then a then statement.
 */
public record WhenThen(Filter when, Entity then) implements Entity {

    /**
     * Creates a WHEN...THEN statement with the provided condition.
     *
     * @param condition a filter to be used in WHEN.
     * @return A new instance of the WhenThen object.
     */
    public static WhenThen when(Filter condition) {
        return new WhenThen(condition, null);
    }

    /**
     * Adds THEN statement to the WhenThen object.
     *
     * @param value a value.
     * @return A new instance with the provided value. The when field is preserved.
     */
    public WhenThen then(Entity value) {
        return new WhenThen(when, value);
    }
}
