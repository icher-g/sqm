package io.sqm.parser.postgresql;

import io.sqm.core.Expression;
import io.sqm.parser.ParserAdapter;

/**
 * PostgreSQL concatenation parser using the PostgreSQL binary-operator parser.
 */
public class ConcatExprParser extends io.sqm.parser.ansi.ConcatExprParser {

    /**
     * Creates a PostgreSQL concatenation-expression parser.
     */
    public ConcatExprParser() {
        /*
          The adapter is used to avoid casting issues when the returned expression
          is evaluated. The ParseResult returned by BinaryOperatorExprParser is expected
          to contain BinaryOperatorExpr but the actual value can be different.
         */
        super(ParserAdapter.widen(Expression.class, new BinaryOperatorExprParser()));
    }
}