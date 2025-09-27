package io.cherlabs.sqlmodel.render.spi;

import java.util.List;

public interface ParamSink {
    int size();

    int add(Object value);             // returns 1-based index

    List<Object> snapshot();           // for SqlText assembly
}
