package io.cherlabs.sqlmodel.core.traits;

import io.cherlabs.sqlmodel.core.CaseColumn;
import io.cherlabs.sqlmodel.core.Column;
import io.cherlabs.sqlmodel.core.WhenThen;

import java.util.List;

/**
 * An interface to get access to {@link CaseColumn#whens()} on top of a {@link io.cherlabs.sqlmodel.core.Column} interface.
 * Use {@link io.cherlabs.sqlmodel.core.views.Columns#whens(Column)} to get them.
 */
public interface HasWhens {
    List<WhenThen> whens();
}
