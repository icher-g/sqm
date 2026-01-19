package io.sqm.render.ansi.spi;

import io.sqm.render.spi.Operators;

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
    public String between() {
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

    /**
     * Gets a representation of ILIKE operator.
     *
     * @return "ILIKE"
     */
    @Override
    public String ilike() {
        return "ILIKE";
    }

    /**
     * Gets a representation of NOT ILIKE operator.
     *
     * @return "NOT ILIKE"
     */
    @Override
    public String notIlike() {
        return "NOT ILIKE";
    }

    /**
     * Gets a representation of SIMILAR TO operator.
     *
     * @return "SIMILAR TO"
     */
    @Override
    public String similarTo() {
        return "SIMILAR TO";
    }

    /**
     * Gets a representation of NOT SIMILAR TO operator.
     *
     * @return "NOT SIMILAR TO"
     */
    @Override
    public String notSimilarTo() {
        return "NOT SIMILAR TO";
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

    @Override
    public String add() {
        return "+";
    }

    @Override
    public String sub() {
        return "-";
    }

    @Override
    public String mul() {
        return "*";
    }

    @Override
    public String div() {
        return "/";
    }

    @Override
    public String neg() {
        return "-";
    }
}

