package io.sqm.render;

import java.util.List;

/**
 * An interface for the render result.
 */
public interface SqlText {
    /**
     * A rendered SQL string.
     *
     * @return a rendered string.
     */
    String sql();

    /**
     * A list of params.
     *
     * @return a list of params.
     */
    List<Object> params();
}
