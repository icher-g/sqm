package io.sqm.render.spi;

/**
 * An interface to provide a string representation of the operators per dialect.
 */
public interface Operators {
    /**
     * Gets an ANSI representation of IN operator.
     *
     * @return "IN"
     */
    String in();

    /**
     * Gets an ANSI representation of NOT IN operator.
     *
     * @return "NOT IN"
     */
    String notIn();

    /**
     * Gets an ANSI representation of BETWEEN operator.
     *
     * @return "BETWEEN"
     */
    String between();

    /**
     * Gets an ANSI representation of = operator.
     *
     * @return "="
     */
    String eq();

    /**
     * Gets an ANSI representation of != operator.
     *
     * @return "<>" or "!="
     */
    String ne();

    /**
     * Gets an ANSI representation of < operator.
     *
     * @return "<"
     */
    String lt();

    /**
     * Gets an ANSI representation of <= operator.
     *
     * @return "<="
     */
    String lte();

    /**
     * Gets an ANSI representation of > operator.
     *
     * @return ">"
     */
    String gt();

    /**
     * Gets an ANSI representation of >= operator.
     *
     * @return ">="
     */
    String gte();

    /**
     * Gets an ANSI representation of LIKE operator.
     *
     * @return "LIKE"
     */
    String like();

    /**
     * Gets an ANSI representation of NOT LIKE operator.
     *
     * @return "NOT LIKE"
     */
    String notLike();

    /**
     * Gets a representation of ILIKE operator.
     *
     * @return "ILIKE"
     */
    String ilike();

    /**
     * Gets a representation of NOT ILIKE operator.
     *
     * @return "NOT ILIKE"
     */
    String notIlike();

    /**
     * Gets a representation of SIMILAR TO operator.
     *
     * @return "SIMILAR TO"
     */
    String similarTo();

    /**
     * Gets a representation of NOT SIMILAR TO operator.
     *
     * @return "NOT SIMILAR TO"
     */
    String notSimilarTo();

    /**
     * Gets an ANSI representation of IS NULL operator.
     *
     * @return "IS NULL"
     */
    String isNull();

    /**
     * Gets an ANSI representation of IS NOT NULL operator.
     *
     * @return "IS NOT NULL"
     */
    String isNotNull();

    /**
     * Gets an ANSI representation of AND operator.
     *
     * @return "AND"
     */
    String and();

    /**
     * Gets an ANSI representation of OR operator.
     *
     * @return "OR"
     */
    String or();

    /**
     * Gets an ANSI representation of NOT operator.
     *
     * @return "NOT"
     */
    String not();

    /**
     * Gets an ANSI representation of + operator.
     *
     * @return "+"
     */
    String add();

    /**
     * Gets an ANSI representation of - operator.
     *
     * @return "-"
     */
    String sub();

    /**
     * Gets an ANSI representation of * operator.
     *
     * @return "*"
     */
    String mul();

    /**
     * Gets an ANSI representation of / operator.
     *
     * @return "/"
     */
    String div();

    /**
     * Gets an ANSI representation of negative operator.
     *
     * @return "-"
     */
    String neg();
}
