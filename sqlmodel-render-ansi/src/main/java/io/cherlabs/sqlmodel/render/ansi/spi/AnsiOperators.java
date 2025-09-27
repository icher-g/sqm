package io.cherlabs.sqlmodel.render.ansi.spi;

import io.cherlabs.sqlmodel.render.spi.Operators;

public final class AnsiOperators implements Operators {

    @Override
    public String in() {
        return "IN";
    }

    @Override
    public String notIn() {
        return "NOT IN";
    }

    @Override
    public String range() {
        return "BETWEEN";
    }

    @Override
    public String eq() {
        return "=";
    }

    @Override
    public String ne() {
        return "<>"; // ANSI standard, not "!="
    }

    @Override
    public String lt() {
        return "<";
    }

    @Override
    public String lte() {
        return "<=";
    }

    @Override
    public String gt() {
        return ">";
    }

    @Override
    public String gte() {
        return ">=";
    }

    @Override
    public String like() {
        return "LIKE";
    }

    @Override
    public String notLike() {
        return "NOT LIKE";
    }

    @Override
    public String isNull() {
        return "IS NULL";
    }

    @Override
    public String isNotNull() {
        return "IS NOT NULL";
    }

    @Override
    public String and() {
        return "AND";
    }

    @Override
    public String or() {
        return "OR";
    }

    @Override
    public String not() {
        return "NOT";
    }
}

