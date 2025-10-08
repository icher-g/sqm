package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.CaseColumn;
import io.cherlabs.sqm.core.Column;
import io.cherlabs.sqm.core.Entity;

/**
 * An interface to access {@link CaseColumn#elseValue()} from the {@link io.cherlabs.sqm.core.Column} interface.
 * Use {@link io.cherlabs.sqm.core.views.Columns#elseValue(Column)} to get it.
 */
public interface HasElseValue {
    Entity elseValue();
}
