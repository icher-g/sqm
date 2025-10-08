package io.cherlabs.sqm.render;

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
     * A list of parameters.
     *
     * @return a list of parameters.
     */
    List<Object> params();
}
