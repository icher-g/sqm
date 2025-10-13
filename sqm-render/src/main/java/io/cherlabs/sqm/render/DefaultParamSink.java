package io.cherlabs.sqm.render;

import io.cherlabs.sqm.render.spi.ParamSink;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link ParamSink}.
 */
public class DefaultParamSink implements ParamSink {
    private final List<Object> values = new ArrayList<>();

    /**
     * Gets the current number of parameters.
     *
     * @return the current number of parameters.
     */
    @Override
    public int size() {
        return values.size();
    }

    /**
     * Adds new parameter.
     *
     * @param value a value to add.
     */
    @Override
    public void add(Object value) {
        values.add(value);
    }

    /**
     * Gets a list of parameters.
     *
     * @return a list of parameters.
     */
    @Override
    public List<Object> snapshot() {
        return values;
    }
}
