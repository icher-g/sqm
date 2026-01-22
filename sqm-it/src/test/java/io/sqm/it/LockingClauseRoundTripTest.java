package io.sqm.it;

import io.sqm.core.Query;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LockingClause Round-Trip Tests")
class LockingClauseRoundTripTest {

    private ParseContext parseContext;
    private RenderContext renderContext;

    @BeforeEach
    void setUp() {
        parseContext = ParseContext.of(new AnsiSpecs());
        renderContext = RenderContext.of(new AnsiDialect());
    }

    @Test
    @DisplayName("Round-trip simple FOR UPDATE")
    void roundTripSimpleForUpdate() {
        String originalSql = "SELECT * FROM users FOR UPDATE";
        
        var parseResult = parseContext.parse(Query.class, originalSql);
        assertTrue(parseResult.ok(), "Parse should succeed");
        
        var query = parseResult.value();
        var renderedSql = renderContext.render(query).sql();
        
        assertEquals(normalizeWhitespace(originalSql), normalizeWhitespace(renderedSql));
    }

    @Test
    @DisplayName("Round-trip FOR UPDATE with WHERE")
    void roundTripForUpdateWithWhere() {
        String originalSql = "SELECT id, name FROM users WHERE active = TRUE FOR UPDATE";
        
        var parseResult = parseContext.parse(Query.class, originalSql);
        assertTrue(parseResult.ok());
        
        var query = parseResult.value();
        var renderedSql = renderContext.render(query).sql();
        
        assertEquals(normalizeWhitespace(originalSql), normalizeWhitespace(renderedSql));
    }

    @Test
    @DisplayName("Round-trip FOR UPDATE with JOIN")
    void roundTripForUpdateWithJoin() {
        String originalSql = "SELECT u.id, o.order_id FROM users AS u " +
            "INNER JOIN orders AS o ON u.id = o.user_id FOR UPDATE";
        
        var parseResult = parseContext.parse(Query.class, originalSql);
        assertTrue(parseResult.ok());
        
        var query = parseResult.value();
        var renderedSql = renderContext.render(query).sql();
        
        assertEquals(normalizeWhitespace(originalSql), normalizeWhitespace(renderedSql));
    }

    @Test
    @DisplayName("Round-trip FOR UPDATE with ORDER BY and LIMIT")
    void roundTripForUpdateWithOrderByAndLimit() {
        String originalSql = "SELECT * FROM users ORDER BY id OFFSET 10 ROWS " +
            "FETCH NEXT 5 ROWS ONLY FOR UPDATE";
        
        var parseResult = parseContext.parse(Query.class, originalSql);
        assertTrue(parseResult.ok());
        
        var query = parseResult.value();
        var renderedSql = renderContext.render(query).sql();
        
        assertEquals(normalizeWhitespace(originalSql), normalizeWhitespace(renderedSql));
    }

    @Test
    @DisplayName("DSL-built query with FOR UPDATE renders correctly")
    void dslBuiltQueryRenders() {
        var query = select(col("*"))
            .from(tbl("users"))
            .lockFor(update(), ofTables(), false, false);
        
        var sql = renderContext.render(query).sql();
        
        assertTrue(normalizeWhitespace(sql).endsWith("FOR UPDATE"));
    }

    @Test
    @DisplayName("Complex query with FOR UPDATE preserves structure")
    void complexQueryPreservesStructure() {
        String originalSql = "SELECT u.id, u.name, COUNT(*) AS cnt " +
            "FROM users AS u " +
            "INNER JOIN orders AS o ON u.id = o.user_id " +
            "WHERE u.active = TRUE " +
            "GROUP BY u.id, u.name " +
            "HAVING COUNT(*) > 5 " +
            "ORDER BY cnt DESC " +
            "FOR UPDATE";
        
        var parseResult = parseContext.parse(Query.class, originalSql);
        assertTrue(parseResult.ok());
        
        var query = parseResult.value();
        var renderedSql = renderContext.render(query).sql();
        
        var normalizedOriginal = normalizeWhitespace(originalSql);
        var normalizedRendered = normalizeWhitespace(renderedSql);
        
        assertEquals(normalizedOriginal, normalizedRendered);
    }

    @Test
    @DisplayName("Query without FOR UPDATE parses and renders correctly")
    void queryWithoutForUpdate() {
        String originalSql = "SELECT * FROM users";
        
        var parseResult = parseContext.parse(Query.class, originalSql);
        assertTrue(parseResult.ok());
        
        var query = parseResult.value();
        var renderedSql = renderContext.render(query).sql();
        
        assertEquals(normalizeWhitespace(originalSql), normalizeWhitespace(renderedSql));
        assertFalse(normalizeWhitespace(renderedSql).contains("FOR UPDATE"));
    }

    @Test
    @DisplayName("Round-trip FOR UPDATE with subquery")
    void roundTripForUpdateWithSubquery() {
        String originalSql = "SELECT * FROM users WHERE id IN ( SELECT user_id FROM orders ) FOR UPDATE";
        
        var parseResult = parseContext.parse(Query.class, originalSql);
        assertTrue(parseResult.ok());
        
        var query = parseResult.value();
        var renderedSql = renderContext.render(query).sql();
        
        assertEquals(normalizeWhitespace(originalSql), normalizeWhitespace(renderedSql));
    }

    @Test
    @DisplayName("Multiple queries - only last one has FOR UPDATE")
    void multipleQueries() {
        String sql1 = "SELECT * FROM users";
        String sql2 = "SELECT * FROM users FOR UPDATE";
        
        var result1 = parseContext.parse(Query.class, sql1);
        var result2 = parseContext.parse(Query.class, sql2);
        
        assertTrue(result1.ok());
        assertTrue(result2.ok());
        
        var rendered1 = renderContext.render(result1.value()).sql();
        var rendered2 = renderContext.render(result2.value()).sql();
        
        assertFalse(normalizeWhitespace(rendered1).contains("FOR UPDATE"));
        assertTrue(normalizeWhitespace(rendered2).contains("FOR UPDATE"));
    }

    private String normalizeWhitespace(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
