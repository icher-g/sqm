package io.sqm.parser.postgresql;

import io.sqm.parser.spi.ParsersRepository;

public class Parsers {

    private static final ParsersRepository defaultRepository = registerDefaults(io.sqm.parser.ansi.Parsers.ansi());

    private Parsers() {
    }

    public static ParsersRepository postgres() {
        return defaultRepository;
    }

    private static ParsersRepository registerDefaults(ParsersRepository r) {
        return r
            .register(new CteDefParser())
            .register(new GroupingSetsParser())
            .register(new GroupingSetParser())
            .register(new RollupParser())
            .register(new CubeParser())
            .register(new OrderItemParser())
            .register(new TableParser())
            .register(new ArrayExprParser())
            .register(new CastExprParser())
            .register(new ArraySubscriptExprParser())
            .register(new ArraySliceExprParser())
            .register(new FunctionTableParser())
            .register(new DistinctSpecParser())
            .register(new LateralParser())
            .register(new LockingClauseParser())
            .register(new RegexPredicateParser());
    }
}
