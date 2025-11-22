package io.sqm.render;

import java.util.List;

/**
 * Represents a render result.
 *
 * @param sql    a rendered string.
 * @param params a list of params if there are any.
 */
public record RenderResult(String sql, List<Object> params) implements SqlText {
}
