package io.cherlabs.sqlmodel.core;

import java.util.List;

public interface Column extends Entity {
    static NamedColumn of(String name) {
        return new NamedColumn(name, null, null);
    }

    static QueryColumn of(Query query) {
        return new QueryColumn(query, null);
    }

    static CaseColumn of(WhenThen when) {
        return CaseColumn.of(when);
    }

    static FunctionColumn func(String name, FunctionColumn.Arg... args) {
        return new FunctionColumn(name, List.of(args), false, null);
    }

    static ExpressionColumn expr(String exp) {
        return new ExpressionColumn(exp, null);
    }
}
