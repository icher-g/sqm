package io.sqm.parser.core;

/**
 * A list of token types.
 */
public enum TokenType {
    // literals & names
    IDENT, NUMBER, STRING,
    QMARK,          // ? - might be an anonymous parameter

    // punctuation
    DOT,
    COMMA,
    LPAREN,         // (
    RPAREN,         // )
    LBRACKET,       // [
    RBRACKET,       // ]
    BACKTICK,       // `
    QUOTE,          // "
    COLON,          // :
    DOUBLE_COLON,   // ::
    DOLLAR,         // $

    // operators
    OPERATOR,

    // keywords (recognized case-insensitively)
    AND, OR, NOT, IN, LIKE, ILIKE, SIMILAR, TO, BETWEEN, SYMMETRIC, IS, NULL, TRUE, FALSE, DISTINCT, EXISTS, ESCAPE, ANY, CAST, ARRAY,

    // join-specific (kept, since core is shared; harmless for filters)
    JOIN, INNER, LEFT, RIGHT, FULL, OUTER, CROSS, USING, NATURAL, ON, AS,

    // window, over
    WINDOW, OVER, PARTITION, RANGE, GROUPS, UNBOUNDED, PRECEDING, FOLLOWING, CURRENT, TIES, NO, OTHERS, WITHIN, FILTER, EXCLUDE,

    // order by specific
    ASC, DESC, NULLS, FIRST, LAST, DEFAULT, COLLATE,

    // case
    CASE, WHEN, THEN, ELSE, END,

    // query
    WITH, WITHOUT, RECURSIVE, SELECT, FROM, LATERAL, WHERE, GROUP, HAVING, ORDER, LIMIT, OFFSET, FETCH, NEXT, ROW, ROWS, ONLY, BY, TOP,
    UNION, INTERSECT, EXCEPT, ALL, VALUES, MATERIALIZED,

    // Locking clause
    FOR, UPDATE, SHARE, OF, NOWAIT, SKIP, LOCKED, KEY,

    EOF
}
