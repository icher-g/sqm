package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.Query;
import io.cherlabs.sqm.core.SelectQuery;
import io.cherlabs.sqm.render.DefaultSqlWriter;
import io.cherlabs.sqm.render.SqlText;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.ansi.spi.AnsiRenderContext;
import io.cherlabs.sqm.render.repos.DefaultRenderersRepository;
import io.cherlabs.sqm.render.spi.RenderContext;
import io.cherlabs.sqm.render.spi.RenderersRepository;

public final class Renderers {

    private static RenderersRepository repository;

    private Renderers() {
    }

    public static SqlText render(Query<?> query) {
        return render(query, new AnsiRenderContext());
    }

    public static SqlText render(Query<?> query, RenderContext ctx) {
        return render(query, ctx, new DefaultSqlWriter(ctx));
    }

    public static SqlText render(Query<?> query, RenderContext ctx, SqlWriter w) {
        var renderer = defaultRepository().get(Query.class);
        renderer.render(query, ctx, w);
        return w.toText(ctx.params().snapshot());
    }

    public static RenderersRepository defaultRepository() {
        if (repository == null) {
            repository = registerDefaults(new DefaultRenderersRepository());
        }
        return repository;
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
