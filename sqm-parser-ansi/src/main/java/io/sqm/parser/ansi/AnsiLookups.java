package io.sqm.parser.ansi;

import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.Lookahead;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.Lookups;

import static io.sqm.parser.core.OperatorTokens.*;

/**
 * A default implementation of the lookups.
 */
public class AnsiLookups implements Lookups {
    /**
     * Creates ANSI lookups.
     */
    public AnsiLookups() {
    }

    private static Lookahead skipParenthesis(Cursor cur, Lookahead p) {
        if (cur.match(TokenType.LPAREN, p.current())) {
            p.increment();
        }
        return p;
    }

    /**
     * Checks whether the next tokens represent a column reference, such as {@code col} or {@code t.col}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a column reference appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeColumnRef(Cursor cur, Lookahead pos) {
        if (looksLikeTypedLiteral(cur, Lookahead.at(pos.current()))) {
            return false;
        }
        var p = Lookahead.at(pos.current());
        if (cur.match(TokenType.IDENT, p.current())) {
            p.increment();
            while (cur.match(TokenType.DOT, p.current())) {
                p.increment();
                if (!cur.match(TokenType.IDENT, p.current())) {
                    return false;
                }
                p.increment();
            }
            if (!cur.match(TokenType.LPAREN, p.current())) {
                pos.increment(p.current() - pos.current());
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the next tokens represent a predicate (a boolean condition).
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a predicate structure is detected, {@code false} otherwise
     */
    @Override
    public boolean looksLikePredicate(Cursor cur, Lookahead pos) {
        // Full expression with proper precedence:
        //    predicate := disjunction.
        //    disjunction := conjunction ('OR' conjunction)*
        //    conjunction := primary ('AND' primary)*
        //    primary := NOT primary | '(' predicate ')' | atomic
        return looksLikeOrPredicate(cur, pos) || looksLikeAndPredicate(cur, pos) || looksLikePrimaryPredicate(cur, pos);
    }

    /**
     * Checks if the next tokens represent a unary predicate (a boolean expression: TRUE, FALSE or a boolean column).
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a predicate structure is detected, {@code false} otherwise
     */
    @Override
    public boolean looksLikeUnaryPredicate(Cursor cur, Lookahead pos) {
        if (cur.matchAny(pos.current(), TokenType.TRUE, TokenType.FALSE)) {
            pos.increment();
            return true;
        }
        return looksLikeColumnRef(cur, pos);
    }

    /**
     * Determines if the next tokens represent an {@code ANY} or {@code ALL} quantified predicate.
     *
     * @param cur the current token cursor
     * @return {@code true} if an {@code ANY} or {@code ALL} predicate appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeAnyAllPredicate(Cursor cur, Lookahead pos) {
        var p = Lookahead.at(pos.current());
        if (looksLikeComparisonPredicate(cur, p)) {
            if (cur.match(TokenType.ANY, p.current()) || cur.match(TokenType.ALL, p.current())) {
                p.increment();
                pos.increment(p.current() - pos.current());
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the upcoming tokens start a {@code BETWEEN} predicate.
     *
     * @param cur the current token cursor
     * @return {@code true} if a {@code BETWEEN} clause appears next, {@code false} otherwise
     */
    @Override
    public boolean looksLikeBetweenPredicate(Cursor cur, Lookahead pos) {
        var p = Lookahead.at(pos.current());
        if (!looksLikeIdentifier(cur, p) || !cur.match(TokenType.BETWEEN, p.current())) {
            return false;
        }
        p.increment();
        if (!looksLikeIdentifier(cur, p) || !cur.match(TokenType.AND, p.current())) {
            return false;
        }
        p.increment();
        pos.increment(p.current() - pos.current());
        return true;
    }

    /**
     * Determines whether the tokens represent a comparison predicate such as {@code a = b} or {@code x > 10}.
     *
     * @param cur the current token cursor
     * @return {@code true} if a comparison predicate seems to begin here, {@code false} otherwise
     */
    @Override
    public boolean looksLikeComparisonPredicate(Cursor cur, Lookahead pos) {
        var p = Lookahead.at(pos.current());
        if ((looksLikeArithmeticOperation(cur, p) || looksLikeFunctionCall(cur, p) || looksLikeIdentifier(cur, p)) && looksLikeComparisonOperator(cur, p)) {
            pos.increment(p.current() - pos.current());
            return true;
        }
        return false;
    }

    /**
     * Checks whether the next tokens indicate an {@code EXISTS} predicate.
     *
     * @param cur the current token cursor
     * @return {@code true} if an {@code EXISTS} clause is detected, {@code false} otherwise
     */
    @Override
    public boolean looksLikeExistsPredicate(Cursor cur, Lookahead pos) {
        var p = Lookahead.at(pos.current());
        if (cur.match(TokenType.EXISTS, p.current())) {
            p.increment();
            pos.increment(p.current() - pos.current());
            return true;
        }
        if (!cur.match(TokenType.NOT, p.current())) {
            return false;
        }
        p.increment();
        if (cur.match(TokenType.EXISTS, p.current())) {
            p.increment();
            pos.increment(p.current() - pos.current());
            return true;
        }
        return false;
    }

    /**
     * Determines if the next tokens form an {@code IN (...)} predicate.
     *
     * @param cur the current token cursor
     * @return {@code true} if an {@code IN} predicate appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeInPredicate(Cursor cur, Lookahead pos) {
        var p = Lookahead.at(pos.current());
        if (!looksLikeIdentifier(cur, p) && !looksLikeRowExpr(cur, p)) {
            return false;
        }
        if (cur.match(TokenType.IN, p.current())) {
            p.increment();
            pos.increment(p.current() - pos.current());
            return true;
        }
        if (!cur.match(TokenType.NOT, p.current())) {
            return false;
        }
        p.increment();
        if (cur.match(TokenType.IN, p.current())) {
            p.increment();
            pos.increment(p.current() - pos.current());
            return true;
        }
        return false;
    }

    /**
     * Checks if the next tokens form an {@code IS [NOT] NULL} predicate.
     *
     * @param cur the current token cursor
     * @return {@code true} if an {@code IS NULL} or {@code IS NOT NULL} expression appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeIsNullPredicate(Cursor cur, Lookahead pos) {
        var p = Lookahead.at(pos.current());
        if (looksLikeIdentifier(cur, p) && cur.match(TokenType.IS, p.current())) {
            p.increment();
            if (cur.match(TokenType.NULL, p.current())) {
                p.increment();
                pos.increment(p.current() - pos.current());
                return true;
            }
            if (cur.match(TokenType.NOT, p.current())) {
                p.increment();
                if (cur.match(TokenType.NULL, p.current())) {
                    p.increment();
                    pos.increment(p.current() - pos.current());
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determines if the next tokens form a {@code LIKE} or {@code NOT LIKE} predicate.
     *
     * @param cur the current token cursor
     * @return {@code true} if a {@code LIKE} clause appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeLikePredicate(Cursor cur, Lookahead pos) {
        var p = Lookahead.at(pos.current());
        if (looksLikeIdentifier(cur, p)) {
            if (cur.match(TokenType.NOT, p.current())) {
                p.increment();
            }
            if (cur.match(TokenType.LIKE, p.current())) {
                p.increment();
                pos.increment(p.current() - pos.current());
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the upcoming tokens start with the {@code NOT} keyword.
     *
     * @param cur the current token cursor
     * @return {@code true} if a {@code NOT} operator appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeNotPredicate(Cursor cur, Lookahead pos) {
        var p = Lookahead.at(pos.current());
        if (cur.match(TokenType.NOT, p.current())) {
            p.increment();
            if (cur.match(TokenType.LPAREN)) {
                p.increment();
                pos.increment(p.current() - pos.current());
                return true;
            }
        }
        return false;
    }

    /**
     * Determines whether the tokens form a logical {@code AND} predicate.
     *
     * @param cur the current token cursor
     * @return {@code true} if an {@code AND} clause is found, {@code false} otherwise
     */
    @Override
    public boolean looksLikeAndPredicate(Cursor cur, Lookahead pos) {
        var p = skipParenthesis(cur, Lookahead.at(pos.current()));
        if (looksLikePrimaryPredicate(cur, p)) {
            return cur.find(p.current(), TokenType.AND) < cur.size();
        }
        return false;
    }

    /**
     * Determines whether the tokens form a logical {@code OR} predicate.
     *
     * @param cur the current token cursor
     * @return {@code true} if an {@code OR} clause is found, {@code false} otherwise
     */
    @Override
    public boolean looksLikeOrPredicate(Cursor cur, Lookahead pos) {
        var p = skipParenthesis(cur, Lookahead.at(pos.current()));
        if (looksLikePrimaryPredicate(cur, p)) {
            return cur.find(p.current(), TokenType.OR) < cur.size();
        }
        return false;
    }

    /**
     * Determines whether the upcoming tokens represent any kind of SQL expression.
     *
     * @param cur the cursor positioned at the current token stream location
     * @param pos the current lookahead position.
     * @return {@code true} if the next tokens appear to form an expression, {@code false} otherwise
     */
    @Override
    public boolean looksLikeExpression(Cursor cur, Lookahead pos) {
        return looksLikeCaseExpr(cur, pos) || looksLikeColumnRef(cur, pos) || looksLikeFunctionCall(cur, pos) ||
            looksLikeLiteralExpr(cur, pos) || looksLikePredicate(cur, pos) || looksLikeValueSet(cur, pos) ||
            looksLikeParam(cur, pos) || looksLikeQueryExpr(cur, pos);
    }

    /**
     * Checks if the upcoming tokens form the beginning of a {@code CASE} expression.
     *
     * @param cur the current token cursor
     * @return {@code true} if the next token is {@code CASE}, {@code false} otherwise
     */
    @Override
    public boolean looksLikeCaseExpr(Cursor cur, Lookahead pos) {
        if (cur.match(TokenType.CASE)) {
            var i = cur.find(pos.current(), TokenType.END);
            pos.increment(i - pos.current());
            return true;
        }
        return false;
    }

    /**
     * Determines if the upcoming tokens indicate a function call, such as {@code COUNT(...)} or {@code ABS(...)}.
     *
     * @param cur the current token cursor
     * @return {@code true} if a function call is likely, {@code false} otherwise
     */
    @Override
    public boolean looksLikeFunctionCall(Cursor cur, Lookahead pos) {
        if (!cur.match(TokenType.IDENT)) {
            return false;
        }
        var p = Lookahead.at(pos.current() + 1); // IDENT
        while (cur.match(TokenType.DOT, p.current()) && cur.match(TokenType.IDENT, p.current() + 1)) {
            p.increment(2);
        }
        if (cur.match(TokenType.LPAREN, p.current())) {
            do {
                p.increment();
            } while (!cur.match(TokenType.RPAREN, p.current()));
            p.increment();
            pos.increment(p.current() - pos.current());
            return true;
        }
        return false;
    }

    /**
     * Checks whether the next tokens represent a value set — for example, a list or row constructor.
     *
     * @param cur the current token cursor
     * @return {@code true} if a value set appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeValueSet(Cursor cur, Lookahead pos) {
        return looksLikeRowExpr(cur, pos) || looksLikeRowListExpr(cur, pos) || looksLikeQueryExpr(cur, pos);
    }

    /**
     * Determines if the tokens start a subquery expression, such as {@code (SELECT ...)}.
     *
     * @param cur the current token cursor
     * @return {@code true} if a subquery expression is likely, {@code false} otherwise
     */
    @Override
    public boolean looksLikeQueryExpr(Cursor cur, Lookahead pos) {
        if (!cur.match(TokenType.LPAREN, pos.current())) {
            return false;
        }
        var p = Lookahead.at(pos.current() + 1); // LPAREN
        if (looksLikeQuery(cur, p)) {
            var i = cur.find(p.current(), TokenType.RPAREN);
            if (i < cur.size()) {
                pos.increment(i - pos.current());
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the next tokens represent a row constructor, such as {@code (a, b)}.
     *
     * @param cur the current token cursor
     * @return {@code true} if a row expression appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeRowExpr(Cursor cur, Lookahead pos) {
        // match (1, or ('a', --> (1,2) OR (1)
        if (!cur.match(TokenType.LPAREN, pos.current())) {
            return false;
        }
        var p = Lookahead.at(pos.current() + 1); // LPAREN
        if (!looksLikeIdentifier(cur, p)) {
            return false;
        }
        if (cur.match(TokenType.COMMA, p.current()) || cur.match(TokenType.RPAREN, p.current())) {
            while (!cur.match(TokenType.RPAREN, p.current())) { // skip to the closing ')'
                p.increment();
            }
            p.increment();
            pos.increment(p.current() - pos.current());
            return true;
        }
        return false;
    }

    /**
     * Determines if the upcoming tokens form a list of row expressions, for example {@code ((1, 2), (3, 4))}.
     *
     * @param cur the current token cursor
     * @return {@code true} if a row list is detected, {@code false} otherwise
     */
    @Override
    public boolean looksLikeRowListExpr(Cursor cur, Lookahead pos) {
        // match ((1, or (('a', --> ((1,2),(3,4))
        if (!cur.match(TokenType.LPAREN, pos.current())) {
            return false;
        }
        var p = Lookahead.at(pos.current() + 1); // LPAREN
        if (!cur.match(TokenType.LPAREN, p.current())) {
            return false;
        }
        p.increment();
        if (!looksLikeLiteralExpr(cur, p)) {
            return false;
        }
        // ((1))
        skipParenthesis(cur, p);
        if (cur.match(TokenType.COMMA, p.current()) || cur.match(TokenType.RPAREN, p.current())) {
            p.increment();
            pos.increment(p.current() - pos.current());
            return true;
        }
        return false;
    }

    /**
     * Checks whether the next token sequence represents a literal value (string, number, boolean, etc.).
     *
     * @param cur the current token cursor
     * @return {@code true} if a literal expression appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeLiteralExpr(Cursor cur, Lookahead pos) {
        if (cur.matchAny(pos.current(), TokenType.NUMBER, TokenType.STRING, TokenType.FALSE, TokenType.TRUE, TokenType.NULL,
            TokenType.BIT_STRING, TokenType.HEX_STRING, TokenType.ESCAPE_STRING, TokenType.DOLLAR_STRING)) {
            pos.increment();
            return true;
        }
        return looksLikeTypedLiteral(cur, pos);
    }

    /**
     * Determines whether the tokens represent a selectable item, such as a column, expression, or {@code *}.
     *
     * @param cur the current token cursor
     * @return {@code true} if a select item is detected, {@code false} otherwise
     */
    @Override
    public boolean looksLikeSelectItem(Cursor cur, Lookahead pos) {
        if (looksLikeStar(cur, pos) || looksLikeQualifiedStar(cur, pos)) {
            return true;
        }
        if (!looksLikeExpression(cur, pos)) {
            return false;
        }
        var p = Lookahead.at(pos.current());
        if (cur.match(TokenType.AS, p.current()) || cur.match(TokenType.IDENT, p.current())) {
            p.increment();
            pos.increment(p.current() - pos.current());
            return true;
        }
        return false;
    }

    /**
     * Checks if the next token is an asterisk ({@code *}), representing all columns.
     *
     * @param cur the current token cursor
     * @return {@code true} if a star appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeStar(Cursor cur, Lookahead pos) {
        if (isStar(cur.peek(pos.current()))) {
            pos.increment();
            return true;
        }
        return false;
    }

    /**
     * Determines whether the tokens represent a qualified star, such as {@code t.*}.
     *
     * @param cur the current token cursor
     * @return {@code true} if a qualified star appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeQualifiedStar(Cursor cur, Lookahead pos) {
        if (!cur.match(TokenType.IDENT, pos.current())) {
            return false;
        }
        var p = Lookahead.at(pos.current() + 1); // IDENT
        if (!cur.match(TokenType.DOT, p.current())) {
            return false;
        }
        p.increment();
        if (isStar(cur.peek(p.current()))) {
            p.increment();
            pos.increment(p.current() - pos.current());
            return true;
        }
        return false;
    }

    /**
     * Checks whether the upcoming tokens represent a full SQL query, possibly including {@code SELECT}, {@code WITH}, or set operations.
     *
     * @param cur the current token cursor
     * @return {@code true} if a query structure appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeQuery(Cursor cur, Lookahead pos) {
        return looksLikeCompositeQuery(cur, pos) || looksLikeSelectQuery(cur, pos) || looksLikeWithQuery(cur, pos);
    }

    /**
     * Checks whether the upcoming tokens represent a {@code SELECT}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a query structure appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeSelectQuery(Cursor cur, Lookahead pos) {
        if (cur.match(TokenType.SELECT, pos.current())) {
            pos.increment();
            return true;
        }
        return false;
    }

    /**
     * Checks if the next tokens represent a {@code WITH} query (Common Table Expression).
     *
     * @param cur the current token cursor
     * @return {@code true} if a {@code WITH} clause appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeWithQuery(Cursor cur, Lookahead pos) {
        if (cur.match(TokenType.WITH, pos.current())) {
            pos.increment();
            return true;
        }
        return false;
    }

    /**
     * Determines whether the upcoming tokens indicate a composite query formed with {@code UNION}, {@code INTERSECT}, or {@code EXCEPT}.
     *
     * @param cur the current token cursor
     * @return {@code true} if a composite query structure is detected, {@code false} otherwise
     */
    @Override
    public boolean looksLikeCompositeQuery(Cursor cur, Lookahead pos) {
        var p = skipParenthesis(cur, Lookahead.at(pos.current()));
        if (looksLikeSelectQuery(cur, p) || looksLikeWithQuery(cur, p)) {
            // use the original pos.current() here so if the composite query is enclosed with () the search
            // will skip everything that is inside the parenthesis.
            return cur.find(Indicators.COMPOSITE_QUERY, pos.current()) < cur.size();
        }
        return false;
    }

    /**
     * Determines if the tokens form a table reference (base table, alias, join, or subquery).
     *
     * @param cur the current token cursor
     * @return {@code true} if a table reference appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeTableRef(Cursor cur, Lookahead pos) {
        return looksLikeQueryTable(cur, pos) || looksLikeValuesTable(cur, pos) || looksLikeTable(cur, pos);
    }

    /**
     * Checks whether the next tokens indicate a subquery table (a query used as a table source).
     *
     * @param cur the current token cursor
     * @return {@code true} if a subquery table appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeQueryTable(Cursor cur, Lookahead pos) {
        if (!cur.match(TokenType.LPAREN, pos.current())) {
            return false;
        }
        var p = Lookahead.at(pos.current() + 1); // LPAREN
        if (looksLikeQuery(cur, p)) {
            pos.increment(p.current() - pos.current());
            return true;
        }
        return false;
    }

    /**
     * Determines if the tokens form a {@code VALUES (...)} table constructor.
     *
     * @param cur the current token cursor
     * @return {@code true} if a {@code VALUES} table appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeValuesTable(Cursor cur, Lookahead pos) {
        // (VALUES (1, 'A'), (2, 'B')) AS v(id, name)
        if (!cur.match(TokenType.LPAREN, pos.current())) {
            return false;
        }
        var p = Lookahead.at(pos.current() + 1); // LPAREN
        if (cur.match(TokenType.VALUES, p.current())) {
            p.increment();
            pos.increment(p.current() - pos.current());
            return true;
        }
        return false;
    }

    /**
     * Checks whether the next tokens represent a base table name or identifier.
     *
     * @param cur the current token cursor
     * @return {@code true} if a simple table reference appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeTable(Cursor cur, Lookahead pos) {
        var p = Lookahead.at(pos.current());
        if (cur.match(TokenType.IDENT, p.current())) {
            p.increment();
            while (cur.match(TokenType.DOT, p.current())) {
                p.increment();
                if (!cur.match(TokenType.IDENT, p.current())) {
                    return false;
                }
                p.increment();
            }
            pos.increment(p.current() - pos.current());
            return true;
        }
        return false;
    }

    /**
     * Determines if the tokens form a {@code JOIN} clause of any kind.
     *
     * @param cur the current token cursor
     * @return {@code true} if a join clause appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeJoin(Cursor cur, Lookahead pos) {
        if (cur.matchAny(Indicators.JOIN, pos.current())) {
            pos.increment();
            return true;
        }
        return false;
    }

    /**
     * Checks whether the next tokens form a {@code CROSS JOIN}.
     *
     * @param cur the current token cursor
     * @return {@code true} if a {@code CROSS JOIN} appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeCrossJoin(Cursor cur, Lookahead pos) {
        if (cur.match(TokenType.CROSS, pos.current())) {
            pos.increment();
            return true;
        }
        return false;
    }

    /**
     * Determines whether the upcoming tokens represent a {@code NATURAL JOIN}.
     *
     * @param cur the current token cursor
     * @return {@code true} if a {@code NATURAL JOIN} appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeNaturalJoin(Cursor cur, Lookahead pos) {
        if (cur.match(TokenType.NATURAL, pos.current())) {
            pos.increment();
            return true;
        }
        return false;
    }

    /**
     * Checks if the next tokens indicate a {@code USING} join clause.
     *
     * @param cur the current token cursor
     * @return {@code true} if a {@code USING} join appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeUsingJoin(Cursor cur, Lookahead pos) {
        if (cur.match(TokenType.USING, pos.current())) {
            pos.increment();
            return true;
        }
        return false;
    }

    /**
     * Determines whether the tokens form an {@code ON} join predicate.
     *
     * @param cur the current token cursor
     * @return {@code true} if an {@code ON} clause appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeOnJoin(Cursor cur, Lookahead pos) {
        if (cur.matchAny(pos.current(), TokenType.JOIN, TokenType.INNER, TokenType.LEFT, TokenType.RIGHT, TokenType.FULL)) {
            pos.increment();
            return true;
        }
        return false;
    }

    /**
     * Determines whether the tokens indicate a query parameter: {@code $1, :name, ?}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a query parameter appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeParam(Cursor cur, Lookahead pos) {
        return looksLikeAnonymousParam(cur, pos) || looksLikeNamedParam(cur, pos) || looksLikeOrdinalParam(cur, pos);
    }

    /**
     * Determines whether the tokens indicate a query parameter: {@code ?}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a query parameter appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeAnonymousParam(Cursor cur, Lookahead pos) {
        if (cur.match(TokenType.QMARK, pos.current())) {
            pos.increment();
            return true;
        }
        return false;
    }

    /**
     * Determines whether the tokens indicate a query parameter: {@code :name}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a query parameter appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeNamedParam(Cursor cur, Lookahead pos) {
        // match ':name' but don't match ':end]' as it is part of the array slice [start:end]
        if (cur.match(TokenType.COLON, pos.current()) && cur.match(TokenType.IDENT, pos.current() + 1) && !cur.match(TokenType.RPAREN, pos.current() + 2)) {
            pos.increment(2);
            return true;
        }
        return false;
    }

    /**
     * Determines whether the tokens indicate a query parameter: {@code $1}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a query parameter appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeOrdinalParam(Cursor cur, Lookahead pos) {
        if (cur.match(TokenType.DOLLAR, pos.current()) && cur.match(TokenType.NUMBER, pos.current() + 1)) {
            pos.increment(2);
            return true;
        }
        return false;
    }

    /**
     * Determines whether the tokens indicate an arithmetic operation: {@code +, -, *, /, %}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if an arithmetic operator appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeArithmeticOperation(Cursor cur, Lookahead pos) {
        var p = Lookahead.at(pos.current());
        if (looksLikeMod(cur, p)) {
            return true;
        }
        if (!looksLikeArithmeticExpr(cur, p)) {
            return false;
        }
        // do not use p as it could skip the '('. look for the operator inside the context.
        // (a + b) should not be found if the cursor is outside the parenthesis.
        return cur.find(t -> isArithmetic(t), pos.current()) < cur.size();
    }

    /**
     * Determines whether the tokens indicate an arithmetic operator: {@code +}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if an arithmetic operator appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeAdd(Cursor cur, Lookahead pos) {
        var p = Lookahead.at(pos.current());
        if (looksLikeArithmeticExpr(cur, p)) {
            // do not use p as it could skip the '('. look for the operator inside the context.
            // (a + b) should not be found if the cursor is outside the parenthesis.
            return cur.find(t -> isPlus(t), pos.current()) < cur.size();
        }
        return false;
    }

    /**
     * Determines whether the tokens indicate an arithmetic operator: {@code -}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if an arithmetic operator appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeSub(Cursor cur, Lookahead pos) {
        var p = Lookahead.at(pos.current());
        if (looksLikeArithmeticExpr(cur, p)) {
            // do not use p as it could skip the '('. look for the operator inside the context.
            // (a - b) should not be found if the cursor is outside the parenthesis.
            int skip = 0;
            if (isMinus(cur.peek(pos.current()))) {
                skip = 1; // this is negative expression, look for the next -.
            }
            return cur.find(t -> isMinus(t), pos.current() + skip) < cur.size();
        }
        return false;
    }

    /**
     * Determines whether the tokens indicate an arithmetic operator: {@code *}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if an arithmetic operator appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeMul(Cursor cur, Lookahead pos) {
        var p = Lookahead.at(pos.current());
        if (looksLikeArithmeticExpr(cur, p)) {
            // do not use p as it could skip the '('. look for the operator inside the context.
            // (a * b) should not be found if the cursor is outside the parenthesis.
            return cur.find(t -> isStar(t), pos.current()) < cur.size();
        }
        return false;
    }

    /**
     * Determines whether the tokens indicate an arithmetic operator: {@code /}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if an arithmetic operator appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeDiv(Cursor cur, Lookahead pos) {
        var p = Lookahead.at(pos.current());
        if (looksLikeArithmeticExpr(cur, p)) {
            // do not use p as it could skip the '('. look for the operator inside the context.
            // (a / b) should not be found if the cursor is outside the parenthesis.
            return cur.find(t -> isSlash(t), pos.current()) < cur.size();
        }
        return false;
    }

    /**
     * Determines whether the tokens indicate an arithmetic operator: {@code %}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if an arithmetic operator appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeMod(Cursor cur, Lookahead pos) {
        if (isPercent(cur.peek(pos.current())) || (cur.match(TokenType.IDENT, pos.current()) && cur.peek(pos.current()).lexeme().equalsIgnoreCase("mod"))) {
            pos.increment();
            return true;
        }
        return false;
    }

    /**
     * Determines whether the tokens indicate an arithmetic operator: {@code -1}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if an arithmetic operator appears ahead, {@code false} otherwise
     */
    @Override
    public boolean looksLikeNeg(Cursor cur, Lookahead pos) {
        if (isMinus(cur.peek(pos.current()))) {
            pos.increment();
            return true;
        }
        return false;
    }

    private boolean looksLikeComparisonOperator(Cursor cur, Lookahead pos) {
        if (isComparison(cur.peek(pos.current()))) {
            pos.increment();
            return true;
        }
        return false;
    }

    /**
     * Determines whether the upcoming tokens indicate literal or column reference.
     *
     * @param cur the current token cursor.
     * @param pos the current token validation position.
     * @return {@code true} if the structure is detected, {@code false} otherwise.
     */
    private boolean looksLikeIdentifier(Cursor cur, Lookahead pos) {
        return looksLikeLiteralExpr(cur, pos) || looksLikeColumnRef(cur, pos);
    }

    private boolean looksLikePrimaryPredicate(Cursor cur, Lookahead pos) {

        // Unary NOT binds tightly: NOT <primary>
        {
            Lookahead p = new Lookahead(pos.current());
            if (cur.match(TokenType.NOT, p.current())) {
                p.increment();
                if (looksLikePrimaryPredicate(cur, p)) {
                    pos.increment(p.current() - pos.current());
                    return true;
                }
            }
        }

        // atomic forms (NO AND/OR here!)
        if (looksLikeAnyAllPredicate(cur, pos)) return true;
        if (looksLikeBetweenPredicate(cur, pos)) return true;
        if (looksLikeComparisonPredicate(cur, pos)) return true;
        if (looksLikeExistsPredicate(cur, pos)) return true;
        if (looksLikeInPredicate(cur, pos)) return true;
        if (looksLikeIsNullPredicate(cur, pos)) return true;
        return looksLikeLikePredicate(cur, pos);
    }

    private boolean looksLikeArithmeticExpr(Cursor cur, Lookahead pos) {
        var p = skipParenthesis(cur, Lookahead.at(pos.current()));
        if (looksLikeCaseExpr(cur, p) || looksLikeColumnRef(cur, p) || looksLikeFunctionCall(cur, p) || looksLikeLiteralExpr(cur, p) ||
            looksLikeQueryExpr(cur, p) || looksLikeNeg(cur, p)) {
            pos.increment(p.current() - pos.current());
            return true;
        }
        return false;
    }

    private boolean looksLikeTypedLiteral(Cursor cur, Lookahead pos) {
        if (!cur.match(TokenType.IDENT, pos.current())) {
            return false;
        }
        String keyword = cur.peek(pos.current()).lexeme();
        if (keyword.equalsIgnoreCase("date")) {
            if (cur.match(TokenType.STRING, pos.current() + 1)) {
                pos.increment(2);
                return true;
            }
            return false;
        }
        if (keyword.equalsIgnoreCase("time") || keyword.equalsIgnoreCase("timestamp")) {
            int p = pos.current() + 1;
            if (cur.matchAny(p, TokenType.WITH, TokenType.WITHOUT)) {
                p++;
                if (!cur.match(TokenType.IDENT, p) || !cur.peek(p).lexeme().equalsIgnoreCase("time")) {
                    return false;
                }
                p++;
                if (!cur.match(TokenType.IDENT, p) || !cur.peek(p).lexeme().equalsIgnoreCase("zone")) {
                    return false;
                }
                p++;
            }
            if (cur.match(TokenType.STRING, p)) {
                pos.increment(p - pos.current() + 1);
                return true;
            }
            return false;
        }
        if (keyword.equalsIgnoreCase("interval")) {
            if (cur.match(TokenType.STRING, pos.current() + 1)) {
                pos.increment(2);
                return true;
            }
            return false;
        }
        return false;
    }
}
