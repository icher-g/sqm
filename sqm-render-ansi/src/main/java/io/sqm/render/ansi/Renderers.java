package io.sqm.render.ansi;

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
            .register(new AndPredicateRenderer())
            .register(new OrPredicateRenderer())
            .register(new AnyAllPredicateRenderer())
            .register(new BetweenPredicateRenderer())
            .register(new CaseExprRenderer())
            .register(new ColumnRefRenderer())
            .register(new ComparisonPredicateRenderer())
            .register(new CompositeQueryRenderer())
            .register(new CrossJoinRenderer())
            .register(new CteDefRenderer())
            .register(new ExistsPredicateRenderer())
            .register(new ExprSelectItemRenderer())
            .register(new FunctionExprRenderer())
            .register(new GroupByRenderer())
            .register(new GroupItemRenderer())
            .register(new InPredicateRenderer())
            .register(new IsNullPredicateRenderer())
            .register(new LikePredicateRenderer())
            .register(new LiteralExprRenderer())
            .register(new NaturalJoinRenderer())
            .register(new NotPredicateRenderer())
            .register(new OnJoinRenderer())
            .register(new OrderByRenderer())
            .register(new OrderItemRenderer())
            .register(new QualifiedStarSelectItemRenderer())
            .register(new QueryExprRenderer())
            .register(new QueryTableRenderer())
            .register(new RowExprRenderer())
            .register(new RowListExprRenderer())
            .register(new SelectQueryRenderer())
            .register(new StarSelectItemRenderer())
            .register(new TableRenderer())
            .register(new UsingJoinRenderer())
            .register(new ValuesTableRenderer())
            .register(new WhenThenRenderer())
            .register(new WithQueryRenderer())
            .register(new FuncExprArgRenderer())
            .register(new FuncStarArgRenderer())
            .register(new WindowDefRenderer())
            .register(new OverSpecDefRenderer())
            .register(new PartitionByRenderer())
            .register(new FrameSpecSingleRenderer())
            .register(new FrameSpecBetweenRenderer())
            .register(new OverSpecRefRenderer())
            .register(new BoundSpecUnboundedPrecedingRenderer())
            .register(new BoundSpecPrecedingRenderer())
            .register(new BoundSpecCurrentRowRenderer())
            .register(new BoundSpecFollowingRenderer())
            .register(new BoundSpecUnboundedFollowingRenderer())
            .register(new NamedParamExprRenderer())
            .register(new OrdinalParamExprRenderer())
            .register(new AnonymousParamExprRenderer())
            .register(new AddArithmeticExprRenderer())
            .register(new DivArithmeticExprRenderer())
            .register(new MulArithmeticExprRenderer())
            .register(new SubArithmeticExprRenderer())
            .register(new ModArithmeticExprRenderer())
            .register(new NegativeArithmeticExprRenderer())
            .register(new BinaryOperatorExprRenderer())
            .register(new UnaryOperatorExprRenderer())
            .register(new CastExprRenderer())
            .register(new ArrayExprRenderer())
            .register(new LimitOffsetRenderer());
    }
}
