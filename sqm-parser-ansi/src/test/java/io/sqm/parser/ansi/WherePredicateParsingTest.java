package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.core.ParserException;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that WHERE clauses are parsed into dedicated Predicate nodes
 * (UnaryPredicate, ComparisonPredicate, LikePredicate, RegexPredicate, etc.)
 * and that ExprPredicate is never produced.
 *
 * <p>These tests are intentionally dialect-facing (PostgreSQL grammar),
 * but the node assertions are dialect-neutral.</p>
 */
final class WherePredicateParsingTest {

    // ------------------------------------------------------------
    // UnaryPredicate: WHERE <expression> (boolean-typed expression)
    // ------------------------------------------------------------

    static Stream<String> unaryPredicateSql() {
        return Stream.of(
            "select * from t where is_active",
            "select * from t where some_function(x)",
            "select * from t where (is_active)",
            "select * from t where true",
            "select * from t where false",
            "select * from t where :p",      // named param (adapt if not supported)
            "select * from t where ?",       // anonymous param (adapt if not supported)
            "select * from t where $1",      // ordinal param (adapt if not supported)
            "select * from t where case when x > 0 then true else false end"
        );
    }

    static Stream<Case> structuredPredicateSql() {
        return Stream.of(
            // comparisons
            c("select * from t where a = 1", ComparisonPredicate.class),
            c("select * from t where a <> 1", ComparisonPredicate.class),
            c("select * from t where a > 1", ComparisonPredicate.class),

            // LIKE family
            c("select * from t where name like 'abc%'", LikePredicate.class),
            c("select * from t where name not like 'abc%'", LikePredicate.class),
            c("select * from t where name ilike 'abc%'", LikePredicate.class),
            c("select * from t where name not ilike 'abc%'", LikePredicate.class),
            c("select * from t where name similar to 'a.*'", LikePredicate.class),
            c("select * from t where name not similar to 'a.*'", LikePredicate.class),
            c("select * from t where name like 'a!_%' escape '!'", LikePredicate.class),

            // null checks
            c("select * from t where a is null", IsNullPredicate.class),
            c("select * from t where a is not null", IsNullPredicate.class),

            // BETWEEN
            c("select * from t where a between 1 and 10", BetweenPredicate.class),
            c("select * from t where a not between 1 and 10", BetweenPredicate.class),

            // IN
            c("select * from t where a in (1,2,3)", InPredicate.class),
            c("select * from t where a not in (1,2,3)", InPredicate.class),
            c("select * from t where a in (select x from t2)", InPredicate.class),

            // EXISTS
            c("select * from t where exists (select 1)", ExistsPredicate.class),
            c("select * from t where not exists (select 1)", ExistsPredicate.class),

            // boolean composition
            c("select * from t where a = 1 and b = 2", AndPredicate.class),
            c("select * from t where a = 1 or b = 2", OrPredicate.class),

            // prefix NOT over a structured predicate
            c("select * from t where not (a = 1)", NotPredicate.class),
            c("select * from t where not is_active", NotPredicate.class),
            c("select * from t where not some_function(x)", NotPredicate.class)
        );
    }

    // ----------------------------------------------------------------
    // Structured predicates: operators / keywords must produce nodes
    // ----------------------------------------------------------------

    private static Case c(String sql, Class<? extends Predicate> expected) {
        return new Case(sql, expected);
    }

    @ParameterizedTest
    @MethodSource("unaryPredicateSql")
    void where_expression_is_parsed_as_unary_predicate(String sql) {
        Query q = parse(sql);
        Predicate where = extractWherePredicate(q);

        assertInstanceOf(UnaryPredicate.class, where, () -> "Expected UnaryPredicate for SQL: " + sql + " but got " + where.getClass().getName());
    }

    // ------------------------------------------------------------
    // Optional: verify regex mapping mode+negation specifically
    // ------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("structuredPredicateSql")
    void where_structured_predicates_have_expected_node_type(Case tc) {
        Query q = parse(tc.sql);
        Predicate where = extractWherePredicate(q);

        assertTrue(tc.expected.isInstance(where),
            () -> "Expected " + tc.expected.getSimpleName() + " for SQL: " + tc.sql
                + " but got " + where.getClass().getName());
    }

    private Query parse(String sql) {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(Query.class, sql);
        if (result.isError()) {
            throw new ParserException(result.errorMessage(), result.problems().getFirst().pos());
        }
        return result.value();
    }

    private Predicate extractWherePredicate(Query q) {
        return ((SelectQuery) q).where();
    }

    private record Case(String sql, Class<? extends Predicate> expected) {
    }
}
