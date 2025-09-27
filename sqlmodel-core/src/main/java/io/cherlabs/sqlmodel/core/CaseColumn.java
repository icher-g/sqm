package io.cherlabs.sqlmodel.core;

import io.cherlabs.sqlmodel.core.traits.HasAlias;
import io.cherlabs.sqlmodel.core.traits.HasElseValue;
import io.cherlabs.sqlmodel.core.traits.HasWhens;

import java.util.List;

public record CaseColumn(List<WhenThen> whens, Entity elseValue, String alias) implements Column, HasWhens, HasElseValue, HasAlias {
    public static CaseColumn of(WhenThen... whens) {
        return new CaseColumn(List.of(whens), null, null);
    }

    public static CaseColumn of(List<WhenThen> whens) {
        return new CaseColumn(whens, null, null);
    }

    public CaseColumn elseValue(Entity elseValue) {
        return new CaseColumn(whens, elseValue, alias);
    }

    public CaseColumn as(String alias) {
        return new CaseColumn(whens, elseValue, alias);
    }

    public static WhenThen when(Filter when, Entity then) {
        return new WhenThen(when, then);
    }
}
