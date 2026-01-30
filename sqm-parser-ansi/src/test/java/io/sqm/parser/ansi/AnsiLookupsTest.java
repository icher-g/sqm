package io.sqm.parser.ansi;

import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.Lookahead;
import io.sqm.parser.spi.IdentifierQuoting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnsiLookupsTest {

    private final AnsiLookups lookups = new AnsiLookups();
    private final IdentifierQuoting quoting = new AnsiSpecs().identifierQuoting();

    @Test
    void detects_column_ref_and_function_call() {
        var col = cursor("t.col");
        var pos = Lookahead.initial();
        assertTrue(lookups.looksLikeColumnRef(col, pos));
        assertTrue(pos.current() > 0);

        var fn = cursor("fn(x)");
        assertFalse(lookups.looksLikeColumnRef(fn, Lookahead.initial()));
        assertTrue(lookups.looksLikeFunctionCall(fn, Lookahead.initial()));
    }

    @Test
    void detects_literals_and_case_expression() {
        assertTrue(lookups.looksLikeLiteralExpr(cursor("123"), Lookahead.initial()));
        assertTrue(lookups.looksLikeLiteralExpr(cursor("TRUE"), Lookahead.initial()));
        assertTrue(lookups.looksLikeCaseExpr(cursor("CASE WHEN a THEN b END"), Lookahead.initial()));
    }

    @Test
    void detects_predicate_forms() {
        assertTrue(lookups.looksLikeComparisonPredicate(cursor("a = b"), Lookahead.initial()));
        assertTrue(lookups.looksLikeAnyAllPredicate(cursor("a = ANY"), Lookahead.initial()));
        assertTrue(lookups.looksLikeBetweenPredicate(cursor("a BETWEEN b AND c"), Lookahead.initial()));
        assertTrue(lookups.looksLikeInPredicate(cursor("a IN"), Lookahead.initial()));
        assertTrue(lookups.looksLikeLikePredicate(cursor("a LIKE b"), Lookahead.initial()));
        assertTrue(lookups.looksLikeIsNullPredicate(cursor("a IS NULL"), Lookahead.initial()));
        assertTrue(lookups.looksLikeExistsPredicate(cursor("EXISTS"), Lookahead.initial()));
        assertTrue(lookups.looksLikeAndPredicate(cursor("a = b AND c = d"), Lookahead.initial()));
        assertTrue(lookups.looksLikeOrPredicate(cursor("a = b OR c = d"), Lookahead.initial()));
        assertTrue(lookups.looksLikeUnaryPredicate(cursor("TRUE"), Lookahead.initial()));
        assertFalse(lookups.looksLikeNotPredicate(cursor("NOT (a)"), Lookahead.initial()));
    }

    @Test
    void detects_query_and_table_forms() {
        assertTrue(lookups.looksLikeSelectQuery(cursor("SELECT"), Lookahead.initial()));
        assertTrue(lookups.looksLikeWithQuery(cursor("WITH"), Lookahead.initial()));
        assertTrue(lookups.looksLikeCompositeQuery(cursor("SELECT 1 UNION SELECT 2"), Lookahead.initial()));

        assertTrue(lookups.looksLikeRowExpr(cursor("(1, 2)"), Lookahead.initial()));
        assertTrue(lookups.looksLikeRowListExpr(cursor("((1, 2), (3, 4))"), Lookahead.initial()));
        assertTrue(lookups.looksLikeQueryExpr(cursor("(SELECT 1)"), Lookahead.initial()));
        assertTrue(lookups.looksLikeQueryTable(cursor("(SELECT 1)"), Lookahead.initial()));
        assertTrue(lookups.looksLikeValuesTable(cursor("(VALUES (1))"), Lookahead.initial()));
        assertTrue(lookups.looksLikeTable(cursor("tbl"), Lookahead.initial()));
        assertTrue(lookups.looksLikeTableRef(cursor("tbl"), Lookahead.initial()));

        assertTrue(lookups.looksLikeJoin(cursor("JOIN"), Lookahead.initial()));
        assertTrue(lookups.looksLikeCrossJoin(cursor("CROSS"), Lookahead.initial()));
        assertTrue(lookups.looksLikeNaturalJoin(cursor("NATURAL"), Lookahead.initial()));
        assertTrue(lookups.looksLikeUsingJoin(cursor("USING"), Lookahead.initial()));
        assertTrue(lookups.looksLikeOnJoin(cursor("LEFT"), Lookahead.initial()));
    }

    @Test
    void detects_select_items() {
        assertTrue(lookups.looksLikeStar(cursor("*"), Lookahead.initial()));
        assertTrue(lookups.looksLikeQualifiedStar(cursor("t.*"), Lookahead.initial()));
        assertTrue(lookups.looksLikeSelectItem(cursor("col AS alias"), Lookahead.initial()));
    }

    @Test
    void detects_params_and_arithmetic() {
        assertTrue(lookups.looksLikeAnonymousParam(cursor("?"), Lookahead.initial()));
        assertTrue(lookups.looksLikeNamedParam(cursor(":name"), Lookahead.initial()));
        assertFalse(lookups.looksLikeNamedParam(cursor(":end)"), Lookahead.initial()));
        assertTrue(lookups.looksLikeOrdinalParam(cursor("$1"), Lookahead.initial()));
        assertTrue(lookups.looksLikeParam(cursor("$1"), Lookahead.initial()));

        assertTrue(lookups.looksLikeArithmeticOperation(cursor("a + b"), Lookahead.initial()));
        assertTrue(lookups.looksLikeAdd(cursor("a + b"), Lookahead.initial()));
        assertTrue(lookups.looksLikeSub(cursor("a - b"), Lookahead.initial()));
        assertTrue(lookups.looksLikeMul(cursor("a * b"), Lookahead.initial()));
        assertTrue(lookups.looksLikeDiv(cursor("a / b"), Lookahead.initial()));
        assertTrue(lookups.looksLikeMod(cursor("%"), Lookahead.initial()));
        assertTrue(lookups.looksLikeMod(cursor("mod"), Lookahead.initial()));
        assertTrue(lookups.looksLikeNeg(cursor("-a"), Lookahead.initial()));
    }

    @Test
    void detects_expression_and_value_set() {
        assertTrue(lookups.looksLikeExpression(cursor("col"), Lookahead.initial()));
        assertTrue(lookups.looksLikeValueSet(cursor("(1, 2)"), Lookahead.initial()));
    }

    private Cursor cursor(String sql) {
        return Cursor.of(sql, quoting);
    }
}
