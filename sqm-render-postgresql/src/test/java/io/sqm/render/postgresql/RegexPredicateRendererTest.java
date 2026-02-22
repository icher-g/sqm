package io.sqm.render.postgresql;

import io.sqm.core.*;
import io.sqm.render.ansi.RegexPredicateRenderer;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PostgreSQL {@link RegexPredicateRenderer}.
 */
@DisplayName("PostgreSQL RegexPredicateRenderer Tests")
class RegexPredicateRendererTest {

    private RenderContext renderContext;

    @BeforeEach
    void setUp() {
        renderContext = RenderContext.of(new PostgresDialect());
    }

    @Test
    @DisplayName("Render case-sensitive regex match")
    void rendersCaseSensitiveMatch() {
        var pred = RegexPredicate.of(RegexMode.MATCH, col("name"), lit("^A.*"), false);
        var sql = renderContext.render(pred).sql();
        
        assertEquals("name ~ '^A.*'", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render case-insensitive regex match")
    void rendersCaseInsensitiveMatch() {
        var pred = RegexPredicate.of(RegexMode.MATCH_INSENSITIVE, col("email"), lit("@example\\.com$"), false);
        var sql = renderContext.render(pred).sql();
        
        assertEquals("email ~* '@example\\.com$'", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render negated case-sensitive regex match")
    void rendersNegatedCaseSensitiveMatch() {
        var pred = RegexPredicate.of(RegexMode.MATCH, col("name"), lit("^test"), true);
        var sql = renderContext.render(pred).sql();
        
        assertEquals("name !~ '^test'", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render negated case-insensitive regex match")
    void rendersNegatedCaseInsensitiveMatch() {
        var pred = RegexPredicate.of(RegexMode.MATCH_INSENSITIVE, col("email"), lit("@spam\\.com$"), true);
        var sql = renderContext.render(pred).sql();
        
        assertEquals("email !~* '@spam\\.com$'", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render regex in WHERE clause")
    void rendersRegexInWhereClause() {
        var query = select(col("*"))
            .from(tbl("users"))
            .where(RegexPredicate.of(RegexMode.MATCH, col("email"), lit("@company\\.com$"), false))
            .build();
        
        var sql = renderContext.render(query).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("email ~ '@company\\.com$'"));
    }

    @Test
    @DisplayName("Render regex with column pattern")
    void rendersRegexWithColumnPattern() {
        var pred = RegexPredicate.of(RegexMode.MATCH, col("name"), col("pattern_col"), false);
        var sql = renderContext.render(pred).sql();
        
        assertEquals("name ~ pattern_col", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render regex combined with AND")
    void rendersRegexCombinedWithAnd() {
        var pred1 = RegexPredicate.of(RegexMode.MATCH, col("name"), lit("^A"), false);
        var pred2 = RegexPredicate.of(RegexMode.MATCH, col("email"), lit("@example\\.com$"), false);
        var combined = pred1.and(pred2);
        
        var sql = renderContext.render(combined).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("name ~ '^A'"));
        assertTrue(normalizeWhitespace(sql).contains("email ~ '@example\\.com$'"));
        assertTrue(normalizeWhitespace(sql).contains("AND"));
    }

    @Test
    @DisplayName("Render regex on qualified column")
    void rendersRegexOnQualifiedColumn() {
        var pred = RegexPredicate.of(RegexMode.MATCH, col("t", "name"), lit("^A.*"), false);
        var sql = renderContext.render(pred).sql();
        
        assertEquals("t.name ~ '^A.*'", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render regex with function value")
    void rendersRegexWithFunctionValue() {
        var func = func("UPPER", arg(col("name")));
        var pred = RegexPredicate.of(RegexMode.MATCH, func, lit("^A.*"), false);
        var sql = renderContext.render(pred).sql();
        
        assertEquals("UPPER(name) ~ '^A.*'", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render regex in HAVING clause")
    void rendersRegexInHavingClause() {
        var query = select(col("category"), func("COUNT", starArg()).as("cnt"))
            .from(tbl("products"))
            .groupBy(group("category"))
            .having(RegexPredicate.of(RegexMode.MATCH_INSENSITIVE, col("category"), lit("^elec"), false))
            .build();
        
        var sql = renderContext.render(query).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("category ~* '^elec'"));
    }

    private String normalizeWhitespace(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
