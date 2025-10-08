package io.cherlabs.sqm.render.ansi.spi;

import io.cherlabs.sqm.render.spi.ParamSink;
import io.cherlabs.sqm.render.spi.RenderContext;
import io.cherlabs.sqm.render.spi.SqlDialect;

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
