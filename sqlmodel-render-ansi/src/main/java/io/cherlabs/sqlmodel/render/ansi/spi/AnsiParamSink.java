package io.cherlabs.sqlmodel.render.ansi.spi;

import io.cherlabs.sqlmodel.render.spi.ParamSink;

import java.util.ArrayList;
import java.util.List;

public class AnsiParamSink implements ParamSink {
    private final List<Object> values = new ArrayList<>();

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public void add(Object value) {
        values.add(value);
    }

    @Override
    public List<Object> snapshot() {
        return values;
    }
}
