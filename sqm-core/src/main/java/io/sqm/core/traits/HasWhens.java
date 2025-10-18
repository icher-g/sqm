package io.sqm.core.traits;

import io.sqm.core.CaseColumn;
import io.sqm.core.Column;
import io.sqm.core.WhenThen;
import io.sqm.core.views.Columns;

import java.util.List;

/**
 * An interface to get access to {@link CaseColumn#whens()} on top of a {@link Column} interface.
 * Use {@link Columns#whens(Column)} to get them.
 */
public interface HasWhens {
    List<WhenThen> whens();
}
