package io.sqm.parser.core;

/**
 * A list of token types.
 */
public enum TokenType {
    /**
     * Identifier token.
     */
    IDENT,
    /**
     * Numeric literal token.
     */
    NUMBER,
    /**
     * String literal token.
     */
    STRING,
    /**
     * Escape string literal token.
     */
    ESCAPE_STRING,
    /**
     * Dollar-quoted string literal token.
     */
    DOLLAR_STRING,
    /**
     * Bit-string literal token.
     */
    BIT_STRING,
    /**
     * Hex-string literal token.
     */
    HEX_STRING,
    /**
     * Question-mark parameter token.
     */
    QMARK,
    /**
     * Dot punctuation token.
     */
    DOT,
    /**
     * Comma punctuation token.
     */
    COMMA,
    /**
     * Left parenthesis token.
     */
    LPAREN,
    /**
     * Right parenthesis token.
     */
    RPAREN,
    /**
     * Left bracket token.
     */
    LBRACKET,
    /**
     * Right bracket token.
     */
    RBRACKET,
    /**
     * Backtick quote token.
     */
    BACKTICK,
    /**
     * Double-quote token.
     */
    QUOTE,
    /**
     * Colon token.
     */
    COLON,
    /**
     * Double-colon token.
     */
    DOUBLE_COLON,
    /**
     * Dollar sign token.
     */
    DOLLAR,
    /**
     * Generic symbolic operator token.
     */
    OPERATOR,
    /**
     * AND keyword token.
     */
    AND,
    /**
     * OR keyword token.
     */
    OR,
    /**
     * NOT keyword token.
     */
    NOT,
    /**
     * IN keyword token.
     */
    IN,
    /**
     * LIKE keyword token.
     */
    LIKE,
    /**
     * ILIKE keyword token.
     */
    ILIKE,
    /**
     * SIMILAR keyword token.
     */
    SIMILAR,
    /**
     * TO keyword token.
     */
    TO,
    /**
     * BETWEEN keyword token.
     */
    BETWEEN,
    /**
     * SYMMETRIC keyword token.
     */
    SYMMETRIC,
    /**
     * IS keyword token.
     */
    IS,
    /**
     * NULL keyword token.
     */
    NULL,
    /**
     * TRUE keyword token.
     */
    TRUE,
    /**
     * FALSE keyword token.
     */
    FALSE,
    /**
     * DISTINCT keyword token.
     */
    DISTINCT,
    /**
     * EXISTS keyword token.
     */
    EXISTS,
    /**
     * ESCAPE keyword token.
     */
    ESCAPE,
    /**
     * ANY keyword token.
     */
    ANY,
    /**
     * CAST keyword token.
     */
    CAST,
    /**
     * ARRAY keyword token.
     */
    ARRAY,
    /**
     * JOIN keyword token.
     */
    JOIN,
    /**
     * INNER keyword token.
     */
    INNER,
    /**
     * LEFT keyword token.
     */
    LEFT,
    /**
     * RIGHT keyword token.
     */
    RIGHT,
    /**
     * FULL keyword token.
     */
    FULL,
    /**
     * OUTER keyword token.
     */
    OUTER,
    /**
     * CROSS keyword token.
     */
    CROSS,
    /**
     * USING keyword token.
     */
    USING,
    /**
     * NATURAL keyword token.
     */
    NATURAL,
    /**
     * ON keyword token.
     */
    ON,
    /**
     * AS keyword token.
     */
    AS,
    /**
     * WINDOW keyword token.
     */
    WINDOW,
    /**
     * OVER keyword token.
     */
    OVER,
    /**
     * PARTITION keyword token.
     */
    PARTITION,
    /**
     * RANGE keyword token.
     */
    RANGE,
    /**
     * GROUPS keyword token.
     */
    GROUPS,
    /**
     * UNBOUNDED keyword token.
     */
    UNBOUNDED,
    /**
     * PRECEDING keyword token.
     */
    PRECEDING,
    /**
     * FOLLOWING keyword token.
     */
    FOLLOWING,
    /**
     * CURRENT keyword token.
     */
    CURRENT,
    /**
     * TIES keyword token.
     */
    TIES,
    /**
     * NO keyword token.
     */
    NO,
    /**
     * OTHERS keyword token.
     */
    OTHERS,
    /**
     * WITHIN keyword token.
     */
    WITHIN,
    /**
     * FILTER keyword token.
     */
    FILTER,
    /**
     * EXCLUDE keyword token.
     */
    EXCLUDE,
    /**
     * ASC keyword token.
     */
    ASC,
    /**
     * DESC keyword token.
     */
    DESC,
    /**
     * NULLS keyword token.
     */
    NULLS,
    /**
     * FIRST keyword token.
     */
    FIRST,
    /**
     * LAST keyword token.
     */
    LAST,
    /**
     * DEFAULT keyword token.
     */
    DEFAULT,
    /**
     * COLLATE keyword token.
     */
    COLLATE,
    /**
     * CASE keyword token.
     */
    CASE,
    /**
     * WHEN keyword token.
     */
    WHEN,
    /**
     * THEN keyword token.
     */
    THEN,
    /**
     * ELSE keyword token.
     */
    ELSE,
    /**
     * END keyword token.
     */
    END,
    /**
     * WITH keyword token.
     */
    WITH,
    /**
     * WITHOUT keyword token.
     */
    WITHOUT,
    /**
     * RECURSIVE keyword token.
     */
    RECURSIVE,
    /**
     * SELECT keyword token.
     */
    SELECT,
    /**
     * FROM keyword token.
     */
    FROM,
    /**
     * LATERAL keyword token.
     */
    LATERAL,
    /**
     * WHERE keyword token.
     */
    WHERE,
    /**
     * GROUP keyword token.
     */
    GROUP,
    /**
     * HAVING keyword token.
     */
    HAVING,
    /**
     * ORDER keyword token.
     */
    ORDER,
    /**
     * LIMIT keyword token.
     */
    LIMIT,
    /**
     * OFFSET keyword token.
     */
    OFFSET,
    /**
     * FETCH keyword token.
     */
    FETCH,
    /**
     * NEXT keyword token.
     */
    NEXT,
    /**
     * ROW keyword token.
     */
    ROW,
    /**
     * ROWS keyword token.
     */
    ROWS,
    /**
     * ONLY keyword token.
     */
    ONLY,
    /**
     * BY keyword token.
     */
    BY,
    /**
     * TOP keyword token.
     */
    TOP,
    /**
     * UNION keyword token.
     */
    UNION,
    /**
     * INTERSECT keyword token.
     */
    INTERSECT,
    /**
     * EXCEPT keyword token.
     */
    EXCEPT,
    /**
     * ALL keyword token.
     */
    ALL,
    /**
     * VALUES keyword token.
     */
    VALUES,
    /**
     * MATERIALIZED keyword token.
     */
    MATERIALIZED,
    /**
     * ORDINALITY keyword token.
     */
    ORDINALITY,
    /**
     * GROUPING keyword token.
     */
    GROUPING,
    /**
     * SETS keyword token.
     */
    SETS,
    /**
     * ROLLUP keyword token.
     */
    ROLLUP,
    /**
     * CUBE keyword token.
     */
    CUBE,
    /**
     * INSERT keyword token.
     */
    INSERT,
    /**
     * DELETE keyword token.
     */
    DELETE,
    /**
     * MERGE keyword token.
     */
    MERGE,
    /**
     * TRUNCATE keyword token.
     */
    TRUNCATE,
    /**
     * REPLACE keyword token.
     */
    REPLACE,
    /**
     * COPY keyword token.
     */
    COPY,
    /**
     * CREATE keyword token.
     */
    CREATE,
    /**
     * ALTER keyword token.
     */
    ALTER,
    /**
     * DROP keyword token.
     */
    DROP,
    /**
     * GRANT keyword token.
     */
    GRANT,
    /**
     * REVOKE keyword token.
     */
    REVOKE,
    /**
     * COMMENT keyword token.
     */
    COMMENT,
    /**
     * RENAME keyword token.
     */
    RENAME,
    /**
     * FOR keyword token.
     */
    FOR,
    /**
     * UPDATE keyword token.
     */
    UPDATE,
    /**
     * SHARE keyword token.
     */
    SHARE,
    /**
     * OF keyword token.
     */
    OF,
    /**
     * NOWAIT keyword token.
     */
    NOWAIT,
    /**
     * SKIP keyword token.
     */
    SKIP,
    /**
     * LOCKED keyword token.
     */
    LOCKED,
    /**
     * KEY keyword token.
     */
    KEY,
    /**
     * End-of-file token.
     */
    EOF
}
