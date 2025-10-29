package io.sqm.parser.ansi;

import io.sqm.parser.*;
import io.sqm.parser.spi.ParsersRepository;

public final class Parsers {

    private static final ParsersRepository defaultRepository = registerDefaults(new DefaultParsersRepository());

    private Parsers() {
    }

    public static ParsersRepository ansi() {
        return defaultRepository;
    }

    private static ParsersRepository registerDefaults(ParsersRepository r) {
        return r
            .register(new QueryParser())
            .register(new WithQueryParser())
            .register(new CompositeQueryParser())
            .register(new SelectQueryParser())
            .register(new CteDefParser())
            .register(new ExpressionParser())
            .register(new CaseExprParser())
            .register(new FunctionExprParser())
            .register(new PredicateParser())
            .register(new ValueSetParser())
            .register(new ColumnRefParser())
            .register(new LiteralExprParser())
            .register(new ExistsPredicateParser())
            .register(new NotPredicateParser())
            .register(new BetweenPredicateParser())
            .register(new IsNullPredicateParser())
            .register(new InPredicateParser())
            .register(new LikePredicateParser())
            .register(new AnyAllPredicateParser())
            .register(new AndPredicateParser())
            .register(new OrPredicateParser())
            .register(new ComparisonPredicateParser())
            .register(new UnaryPredicateParser())
            .register(new QueryExprParser())
            .register(new RowExprParser())
            .register(new RowListExprParser())
            .register(new SelectItemParser())
            .register(new StarSelectItemParser())
            .register(new QualifiedStarSelectItemParser())
            .register(new ExprSelectItemParser())
            .register(new JoinParser())
            .register(new CrossJoinParser())
            .register(new NaturalJoinParser())
            .register(new UsingJoinParser())
            .register(new OnJoinParser())
            .register(new TableRefParser())
            .register(new QueryTableParser())
            .register(new ValuesTableParser())
            .register(new TableParser())
            .register(new GroupByParser())
            .register(new GroupItemParser())
            .register(new OrderByParser())
            .register(new OrderItemParser())
            .register(new WhenThenParser())
            .register(new LimitOffsetParser())
            .register(new FuncCallArgParser())
            .register(new FuncColumnArgParser())
            .register(new FuncLiteralArgParser())
            .register(new FuncStarArgParser())
            .register(new FunctionExprArgParser());
    }
}
