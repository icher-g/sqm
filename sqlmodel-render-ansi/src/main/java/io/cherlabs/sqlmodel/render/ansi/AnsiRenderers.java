package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.*;
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
                .register(NamedColumn.class, new NamedColumnRenderer())
                .register(ExpressionColumn.class, new ExpressionColumnRenderer())
                .register(QueryColumn.class, new QueryColumnRenderer())
                .register(Query.class, new QueryRenderer())
                .register(ColumnFilter.class, new ColumnFilterRenderer())
                .register(TupleFilter.class, new TupleFilterRenderer())
                .register(CompositeFilter.class, new CompositeFilterRenderer())
                .register(ExpressionFilter.class, new ExpressionFilterRenderer())
                .register(TableJoin.class, new TableJoinRenderer())
                .register(NamedTable.class, new NamedTableRenderer())
                .register(ExpressionJoin.class, new ExpressionJoinRenderer())
                .register(FunctionColumn.class, new FunctionColumnRenderer())
                .register(FunctionColumn.Arg.Column.class, new ColumnArgRenderer())
                .register(FunctionColumn.Arg.Literal.class, new LiteralArgRenderer())
                .register(FunctionColumn.Arg.Function.class, new FunctionArgRenderer())
                .register(FunctionColumn.Arg.Star.class, new StarArgRenderer())
                .register(Group.class, new GroupItemRenderer())
                .register(Order.class, new OrderItemRenderer())
                .register(Values.ListValues.class, new ValuesListRenderer())
                .register(Values.Range.class, new ValuesRangeRenderer())
                .register(Values.Single.class, new ValuesSingleRenderer())
                .register(Values.Subquery.class, new ValuesSubqueryRenderer())
                .register(Values.Tuples.class, new ValuesTuplesRenderer())
                .register(Values.Column.class, new ValuesColumnRenderer())
                .register(CaseColumn.class, new CaseColumnRenderer());
    }
}
