package io.cherlabs.sqlmodel.parser;

import io.cherlabs.sqlmodel.core.*;
import io.cherlabs.sqlmodel.parser.repos.DefaultSpecParsersRepository;
import io.cherlabs.sqlmodel.parser.repos.SpecParsersRepository;

public final class SpecParsers {
    private SpecParsers() {
    }

    public static SpecParsersRepository defaultRepository() {
        return registerDefaults(new DefaultSpecParsersRepository());
    }

    private static SpecParsersRepository registerDefaults(SpecParsersRepository r) {
        return r
                .register(Column.class, new ColumnSpecParser())
                .register(Filter.class, new FilterSpecParser())
                .register(Join.class, new JoinSpecParser())
                .register(Table.class, new TableSpecParser())
                .register(Order.class, new OrderItemSpecParser())
                .register(Group.class, new GroupItemSpecParser());
    }
}
