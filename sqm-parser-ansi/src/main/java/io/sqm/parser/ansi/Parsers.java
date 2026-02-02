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
        var atomicExprParser = new AtomicExprParser();
        var atomicPredicateParser = new AtomicPredicateParser();
        var atomicQueryParser = new AtomicQueryParser();
        var postfixExprParser = new PostfixExprParser(atomicExprParser);
        return r
            .register(new QueryParser())
            .register(new WithQueryParser())
            .register(new CompositeQueryParser(atomicQueryParser))
            .register(new SelectQueryParser())
            .register(new CteDefParser())
            .register(new ExpressionParser())
            .register(new CaseExprParser())
            .register(new FunctionExprParser())
            .register(new PredicateParser())
            .register(new ValueSetParser())
            .register(new ColumnExprParser())
            .register(new LiteralExprParser())
            .register(new DateLiteralExprParser())
            .register(new TimeLiteralExprParser())
            .register(new TimestampLiteralExprParser())
            .register(new IntervalLiteralExprParser())
            .register(new BitStringLiteralExprParser())
            .register(new HexStringLiteralExprParser())
            .register(new EscapeStringLiteralExprParser())
            .register(new DollarStringLiteralExprParser())
            .register(new ExistsPredicateParser())
            .register(new NotPredicateParser(atomicPredicateParser))
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
            .register(new GroupingSetsParser())
            .register(new GroupingSetParser())
            .register(new RollupParser())
            .register(new CubeParser())
            .register(new GroupItemParser())
            .register(new SimpleGroupItemParser())
            .register(new OrderByParser())
            .register(new OrderItemParser())
            .register(new WhenThenParser())
            .register(new LimitOffsetParser())
            .register(new FuncExprArgParser())
            .register(new FuncStarArgParser())
            .register(new FunctionExprArgParser())
            .register(new BoundSpecParser())
            .register(new BoundSpecCurrentRowParser())
            .register(new BoundSpecPrecedingParser())
            .register(new BoundSpecFollowingParser())
            .register(new BoundSpecUnboundedFollowingParser())
            .register(new BoundSpecUnboundedPrecedingParser())
            .register(new FrameSpecParser())
            .register(new FrameSpecSingleParser())
            .register(new FrameSpecBetweenParser())
            .register(new OverSpecParser())
            .register(new OverSpecDefParser())
            .register(new OverSpecRefParser())
            .register(new PartitionByParser())
            .register(new WindowDefParser())
            .register(new ParamExprParser())
            .register(new AnonymousParamExprParser())
            .register(new NamedParamExprParser())
            .register(new OrdinalParamExprParser())
            .register(new ArithmeticExprParser())
            .register(new NegativeArithmeticExprParser(postfixExprParser))
            .register(new ModArithmeticExprParser(postfixExprParser))
            .register(new AddArithmeticExprParser())
            .register(new SubArithmeticExprParser())
            .register(new MulArithmeticExprParser(postfixExprParser))
            .register(new DivArithmeticExprParser(postfixExprParser))
            .register(new MultiplicativeArithmeticExprParser(postfixExprParser))
            .register(new AdditiveArithmeticExprParser())
            .register(new ArrayExprParser())
            .register(new BinaryOperatorExprParser())
            .register(new UnaryOperatorExprParser())
            .register(new CastExprParser())
            .register(new TypeNameParser())
            .register(new RegexPredicateParser())
            .register(new IsDistinctFromPredicateParser())
            .register(new LateralParser())
            .register(new FunctionTableParser())
            .register(new LockingClauseParser())
            .register(new ArraySubscriptExprParser(atomicExprParser))
            .register(new ArraySliceExprParser(atomicExprParser))
            .register(new DistinctSpecParser());
    }
}
