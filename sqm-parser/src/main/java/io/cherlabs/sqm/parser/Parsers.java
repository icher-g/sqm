package io.cherlabs.sqm.parser;

import io.cherlabs.sqm.parser.repos.DefaultParsersRepository;
import io.cherlabs.sqm.parser.repos.ParsersRepository;

public final class Parsers {
    private Parsers() {
    }

    public static ParsersRepository defaultRepository() {
        return registerDefaults(new DefaultParsersRepository());
    }

    private static ParsersRepository registerDefaults(ParsersRepository r) {
        return r
                .register(new ColumnParser(r))
                .register(new FilterParser())
                .register(new JoinParser(r))
                .register(new TableParser())
                .register(new OrderParser(r))
                .register(new GroupParser(r))
                .register(new QueryParser(r));
    }
}
