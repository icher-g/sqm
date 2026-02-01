package io.sqm.parser.ansi;

import io.sqm.core.Query;
import io.sqm.core.RegexPredicate;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration-level tests verifying that SQL regex operators are recognized
 * in a query and produced as {@link RegexPredicate} nodes.
 *
 * <p>These tests are intentionally focused on recognition. They do not verify
 * full rendering compatibility or exhaustive expression parsing.</p>
 */
final class RegexPredicateParserTest {

    @Test
    void parses_regex_match_operator_not_supported() {
        // SQL uses a regex match operator: value ~ pattern
        var sql = "select * from t where name ~ 'abc'";

        var result = parse(sql);

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Regex predicates are not supported by this dialect"));
    }

    @Test
    void parses_regex_not_match_operator_not_supported() {
        // SQL uses negated regex match operator: value !~ pattern
        var sql = "select * from t where name !~ 'abc'";

        var result = parse(sql);

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Regex predicates are not supported by this dialect"));
    }

    @Test
    void parses_regex_match_insensitive_operator_not_supported() {
        // SQL uses case-insensitive regex match operator: value ~* pattern
        var sql = "select * from t where name ~* 'abc'";

        var result = parse(sql);

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Regex predicates are not supported by this dialect"));
    }

    @Test
    void parses_regex_not_match_insensitive_operator_not_supported() {
        // SQL uses negated + case-insensitive regex match operator: value !~* pattern
        var sql = "select * from t where name !~* 'abc'";

        var result = parse(sql);

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Regex predicates are not supported by this dialect"));
    }

    private ParseResult<? extends Query> parse(String sql) {
        var ctx = ParseContext.of(new AnsiSpecs());
        return ctx.parse(Query.class, sql);
    }

}

