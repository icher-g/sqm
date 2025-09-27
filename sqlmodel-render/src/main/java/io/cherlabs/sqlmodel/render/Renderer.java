package io.cherlabs.sqlmodel.render;

import io.cherlabs.sqlmodel.core.Entity;
import io.cherlabs.sqlmodel.core.repos.Handler;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

public interface Renderer<T extends Entity> extends Handler<T> {
    void render(T entity, RenderContext ctx, SqlWriter w);
}
