package io.sqm.core.traits;

import io.sqm.core.CaseColumn;
import io.sqm.core.Column;
import io.sqm.core.Entity;
import io.sqm.core.views.Columns;

/**
 * An interface to access {@link CaseColumn#elseValue()} from the {@link Column} interface.
 * Use {@link Columns#elseValue(Column)} to get it.
 */
public interface HasElseValue {
    Entity elseValue();
}
