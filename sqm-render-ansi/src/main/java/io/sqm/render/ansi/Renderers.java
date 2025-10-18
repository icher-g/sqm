package io.sqm.render.ansi;

import io.sqm.render.ansi.arg.ColumnArgRenderer;
import io.sqm.render.ansi.arg.FunctionArgRenderer;
import io.sqm.render.ansi.arg.LiteralArgRenderer;
import io.sqm.render.ansi.arg.StarArgRenderer;
import io.sqm.render.ansi.column.*;
import io.sqm.render.ansi.filter.ColumnFilterRenderer;
import io.sqm.render.ansi.filter.CompositeFilterRenderer;
import io.sqm.render.ansi.filter.ExpressionFilterRenderer;
import io.sqm.render.ansi.filter.TupleFilterRenderer;
import io.sqm.render.ansi.join.ExpressionJoinRenderer;
import io.sqm.render.ansi.join.TableJoinRenderer;
import io.sqm.render.ansi.query.CompositeQueryRenderer;
import io.sqm.render.ansi.query.CteQueryRenderer;
import io.sqm.render.ansi.query.SelectQueryRenderer;
import io.sqm.render.ansi.query.WithQueryRenderer;
import io.sqm.render.ansi.statement.GroupByRenderer;
import io.sqm.render.ansi.statement.GroupRenderer;
import io.sqm.render.ansi.statement.OrderByRenderer;
import io.sqm.render.ansi.statement.OrderRenderer;
import io.sqm.render.ansi.table.NamedTableRenderer;
import io.sqm.render.ansi.table.QueryTableRenderer;
import io.sqm.render.ansi.value.*;
import io.sqm.render.repos.DefaultRenderersRepository;
import io.sqm.render.spi.RenderersRepository;

public final class Renderers {

    private static RenderersRepository repository;

    private Renderers() {
    }

    public static RenderersRepository ansi() {
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
            .register(new SelectQueryRenderer())
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
            .register(new QueryTableRenderer())
            .register(new ValueColumnRenderer())
            .register(new StarColumnRenderer())
            .register(new GroupByRenderer())
            .register(new OrderByRenderer());
    }
}
