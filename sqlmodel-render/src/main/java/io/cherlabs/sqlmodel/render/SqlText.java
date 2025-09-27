package io.cherlabs.sqlmodel.render;

import java.util.List;

public interface SqlText {
    String sql();
    List<Object> params();
}
