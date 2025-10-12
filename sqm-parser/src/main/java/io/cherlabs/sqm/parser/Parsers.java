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
                .register(new ColumnParser())
                .register(new FilterParser())
                .register(new JoinParser(r))
                .register(new TableParser())
                .register(new OrderParser())
                .register(new GroupParser())
                .register(new QueryParser(r));
    }
}
