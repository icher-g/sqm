package io.sqm.core;

/**
 * Set operators used in composite queries.
 */
public enum SetOperator {
    /** UNION set operator. */
    UNION,
    /** UNION ALL set operator. */
    UNION_ALL,
    /** INTERSECT set operator. */
    INTERSECT,
    /** INTERSECT ALL set operator. */
    INTERSECT_ALL,
    /** EXCEPT set operator. */
    EXCEPT,
    /** EXCEPT ALL set operator. */
    EXCEPT_ALL
}
