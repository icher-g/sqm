package io.cherlabs.sqlmodel.render;

import java.util.List;

public record RenderResult(String sql, List<Object> params) implements SqlText {
}
