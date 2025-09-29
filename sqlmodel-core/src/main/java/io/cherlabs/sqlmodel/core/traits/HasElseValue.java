package io.cherlabs.sqlmodel.core.traits;

import io.cherlabs.sqlmodel.core.CaseColumn;
import io.cherlabs.sqlmodel.core.Column;
import io.cherlabs.sqlmodel.core.Entity;

/**
 * An interface to access {@link CaseColumn#elseValue()} from the {@link io.cherlabs.sqlmodel.core.Column} interface.
 * Use {@link io.cherlabs.sqlmodel.core.views.Columns#elseValue(Column)} to get it.
 */
public interface HasElseValue {
    Entity elseValue();
}
