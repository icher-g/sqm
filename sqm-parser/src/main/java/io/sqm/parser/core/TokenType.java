package io.sqm.parser.core;

/**
 * A list of token types.
 */
public enum TokenType {
    // literals & names
    IDENT, NUMBER, STRING,
    PARAM_QMARK,    // ? - anonymous parameter
    PARAM_POS,      // $1 - positional parameter
    PARAM_NAMED,    // :name or @name - named parameter

    // punctuation
    DOT, COMMA, LPAREN, RPAREN,

    // arithmetic
    PLUS, MINUS, STAR, SLASH, CARET,

    // comparison
    EQ, NEQ1, NEQ2, LT, LTE, GT, GTE,

    // keywords (recognized case-insensitively)
    AND, OR, NOT, IN, LIKE, BETWEEN, SYMMETRIC, IS, NULL, TRUE, FALSE, DISTINCT, EXISTS, ESCAPE, ANY,

    // join-specific (kept, since core is shared; harmless for filters)
    JOIN, INNER, LEFT, RIGHT, FULL, OUTER, CROSS, USING, NATURAL, ON, AS,

    // window, over
    WINDOW, OVER, PARTITION, RANGE, GROUPS, UNBOUNDED, PRECEDING, FOLLOWING, CURRENT, TIES, NO, OTHERS, WITHIN, FILTER, EXCLUDE,

    // order by specific
    ASC, DESC, NULLS, FIRST, LAST, DEFAULT, COLLATE,

    // case
    CASE, WHEN, THEN, ELSE, END,

    // query
    WITH, RECURSIVE, SELECT, FROM, WHERE, GROUP, HAVING, ORDER, LIMIT, OFFSET, FETCH, NEXT, ROW, ROWS, ONLY, BY, TOP,
    UNION, INTERSECT, EXCEPT, ALL, VALUES,

    EOF
}
