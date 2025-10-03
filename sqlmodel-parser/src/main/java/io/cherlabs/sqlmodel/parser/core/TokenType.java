package io.cherlabs.sqlmodel.parser.core;

/**
 * A list of token types.
 */
public enum TokenType {
    // literals & names
    IDENT, NUMBER, STRING, PARAM_QMARK,       // ?
    PARAM_NAMED,       // :name or @name

    // punctuation
    DOT, COMMA, LPAREN, RPAREN,

    // arithmetic
    PLUS, MINUS, STAR, SLASH, CARET,

    // comparison
    EQ, NEQ1, NEQ2, LT, LTE, GT, GTE,

    // keywords (recognized case-insensitively)
    AND, OR, NOT, IN, LIKE, BETWEEN, IS, NULL, TRUE, FALSE, DISTINCT,

    // join-specific (kept, since core is shared; harmless for filters)
    JOIN, INNER, LEFT, RIGHT, FULL, OUTER, CROSS, ON, AS,

    // order by specific
    ASC, DESC, NULLS, FIRST, LAST, DEFAULT, COLLATE,

    // case
    CASE, WHEN, THEN, ELSE, END,

    EOF
}
