package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.Query;
import io.cherlabs.sqlmodel.core.SelectQuery;
import io.cherlabs.sqlmodel.render.repos.DefaultRenderersRepository;
import io.cherlabs.sqlmodel.render.spi.RenderersRepository;

public final class AnsiRenderers {

    private AnsiRenderers() {
    }

    public static RenderersRepository defaultRepository() {
        return registerDefaults(new DefaultRenderersRepository());
    }

    private static RenderersRepository registerDefaults(RenderersRepository r) {
        return r
                .register(new NamedColumnRenderer())
                .register(new ExpressionColumnRenderer())
                .register(new QueryColumnRenderer())
                .register(Query.class, new QueryRenderer())
                .register(SelectQuery.class, new QueryRenderer())
                .register(new ColumnFilterRenderer())
                .register(new TupleFilterRenderer())
                .register(new CompositeFilterRenderer())
                .register(new ExpressionFilterRenderer())
                .register(new TableJoinRenderer())
                .register(new NamedTableRenderer())
                .register(new ExpressionJoinRenderer())
                .register(new FunctionColumnRenderer())
                .register(new ColumnArgRenderer())
                .register(new LiteralArgRenderer())
                .register(new FunctionArgRenderer())
                .register(new StarArgRenderer())
                .register(new GroupRenderer())
                .register(new OrderRenderer())
                .register(new ValuesListRenderer())
                .register(new ValuesRangeRenderer())
                .register(new ValuesSingleRenderer())
                .register(new ValuesSubqueryRenderer())
                .register(new ValuesTuplesRenderer())
                .register(new ValuesColumnRenderer())
                .register(new CaseColumnRenderer())
                .register(new CompositeQueryRenderer())
                .register(new WithQueryRenderer())
                .register(new CteQueryRenderer())
                .register(new QueryTableRenderer());
    }
}
