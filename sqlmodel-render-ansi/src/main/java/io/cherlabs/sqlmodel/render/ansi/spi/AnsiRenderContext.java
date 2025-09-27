package io.cherlabs.sqlmodel.render.ansi.spi;

import io.cherlabs.sqlmodel.render.spi.ParamSink;
import io.cherlabs.sqlmodel.render.spi.RenderContext;
import io.cherlabs.sqlmodel.render.spi.SqlDialect;

public class AnsiRenderContext implements RenderContext {
    private final SqlDialect dialect = new AnsiSqlDialect();
    private final ParamSink params = new AnsiParamSink();

    @Override
    public SqlDialect dialect() {
        return dialect;
    }

    @Override
    public ParamSink params() {
        return params;
    }
}
