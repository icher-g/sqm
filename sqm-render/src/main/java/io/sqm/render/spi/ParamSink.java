package io.sqm.render.spi;

import java.util.List;

/**
 * An interface to bind parameters.
 */
public interface ParamSink {
    /**
     * Gets the current size of the parameters list.
     *
     * @return a size of the parameters list.
     */
    int size();

    /**
     * Adds a value to the parameters list and returns it index.
     * <p>Note: the index is 1-based.</p>
     *
     * @param value a value to add.
     */
    void add(Object value);

    /**
     * Gets the current list of the parameters.
     *
     * @return a parameters list.
     */
    List<Object> snapshot();
}
