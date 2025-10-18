package io.sqm.core;

import io.sqm.core.traits.HasAlias;
import io.sqm.core.traits.HasElseValue;
import io.sqm.core.traits.HasWhens;

import java.util.List;

/**
 * Represents a CASE WHEN...THEN ELSE END statement.
 *
 * @param whens     a list of WHEN...THEN statements.
 * @param elseValue an else value if presented.
 * @param alias     an alias.
 */
public record CaseColumn(List<WhenThen> whens, Entity elseValue, String alias) implements Column, HasWhens, HasElseValue, HasAlias {

    /**
     * Creates an instance of CaseColumn.
     *
     * @param whens an array of WHEN...THEN statements.
     * @return a newly created instance.
     */
    public static CaseColumn of(WhenThen... whens) {
        return new CaseColumn(List.of(whens), null, null);
    }

    /**
     * Creates an instance of CaseColumn.
     *
     * @param whens a list of WHEN...THEN statements.
     * @return a newly created instance.
     */
    public static CaseColumn of(List<WhenThen> whens) {
        return new CaseColumn(whens, null, null);
    }

    /**
     * Creates an instance of WhenThen.
     *
     * @param when the 'when' statement represented by the {@link Filter}.
     * @param then the 'then' statement represented by the {@link Entity}.
     * @return a newly created instance.
     */
    public static WhenThen when(Filter when, Entity then) {
        return new WhenThen(when, then);
    }

    /**
     * Creates a new instance of CaseColumn preserving current values of {@link CaseColumn#whens} and {@link CaseColumn#alias}.
     *
     * @param elseValue statement represented by the {@link Entity}.
     * @return a newly created instance.
     */
    public CaseColumn elseValue(Entity elseValue) {
        return new CaseColumn(whens, elseValue, alias);
    }

    /**
     * Creates a new instance of CaseColumn preserving current values of {@link CaseColumn#whens} and {@link CaseColumn#elseValue}.
     *
     * @param alias an alias.
     * @return a newly created instance.
     */
    public CaseColumn as(String alias) {
        return new CaseColumn(whens, elseValue, alias);
    }
}
