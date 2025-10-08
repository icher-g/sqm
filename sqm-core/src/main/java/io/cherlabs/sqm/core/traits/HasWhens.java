package io.cherlabs.sqm.core.traits;

import io.cherlabs.sqm.core.CaseColumn;
import io.cherlabs.sqm.core.Column;
import io.cherlabs.sqm.core.WhenThen;

import java.util.List;

/**
 * An interface to get access to {@link CaseColumn#whens()} on top of a {@link io.cherlabs.sqm.core.Column} interface.
 * Use {@link io.cherlabs.sqm.core.views.Columns#whens(Column)} to get them.
 */
public interface HasWhens {
    List<WhenThen> whens();
}
