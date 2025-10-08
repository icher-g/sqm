package io.cherlabs.sqm.parser;

import io.cherlabs.sqm.parser.repos.DefaultSpecParsersRepository;
import io.cherlabs.sqm.parser.repos.SpecParsersRepository;

public final class SpecParsers {
    private SpecParsers() {
    }

    public static SpecParsersRepository defaultRepository() {
        return registerDefaults(new DefaultSpecParsersRepository());
    }

    private static SpecParsersRepository registerDefaults(SpecParsersRepository r) {
        return r
                .register(new ColumnSpecParser())
                .register(new FilterSpecParser())
                .register(new JoinSpecParser())
                .register(new TableSpecParser())
                .register(new OrderSpecParser())
                .register(new GroupSpecParser());
    }
}
