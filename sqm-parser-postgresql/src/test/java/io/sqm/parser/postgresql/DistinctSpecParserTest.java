package io.sqm.parser.postgresql;

import io.sqm.core.*;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parser tests for PostgreSQL {@link DistinctSpec}.
 *
 * <p>Tests DISTINCT variations:</p>
 * <ul>
 *   <li>DISTINCT</li>
 *   <li>DISTINCT ON (expr1, expr2, ...)</li>
 * </ul>
 */
@DisplayName("PostgreSQL DistinctSpecParser Tests")
class DistinctSpecParserTest {

    private final ParseContext parseContext = ParseContext.of(new PostgresSpecs());

    @Test
    @DisplayName("Parse simple DISTINCT")
    void parsesSimpleDistinct() {
        var qResult = parseQuery("SELECT DISTINCT name FROM users");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        
        assertNotNull(query.distinct());
    }

    @Test
    @DisplayName("Parse DISTINCT ON with single column")
    void parsesDistinctOnSingleColumn() {
        var qResult = parseQuery("SELECT DISTINCT ON (user_id) * FROM orders ORDER BY user_id, created_at DESC");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        
        assertNotNull(query.distinct());
    }

    @Test
    @DisplayName("Parse DISTINCT ON with multiple columns")
    void parsesDistinctOnMultipleColumns() {
        var qResult = parseQuery("SELECT DISTINCT ON (user_id, product_id) * FROM orders");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        
        assertNotNull(query.distinct());
    }

    @Test
    @DisplayName("Parse DISTINCT ON with qualified columns")
    void parsesDistinctOnWithQualifiedColumns() {
        var qResult = parseQuery("SELECT DISTINCT ON (u.id) u.name FROM users u");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        
        assertNotNull(query.distinct());
    }

    @Test
    @DisplayName("Parse DISTINCT ON with expressions")
    void parsesDistinctOnWithExpressions() {
        var qResult = parseQuery("SELECT DISTINCT ON (LOWER(name)) name FROM users");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        
        assertNotNull(query.distinct());
    }

    @Test
    @DisplayName("Parse DISTINCT with aggregate function")
    void parsesDistinctWithAggregate() {
        var qResult = parseQuery("SELECT DISTINCT COUNT(*) FROM users GROUP BY department");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        
        assertNotNull(query.distinct());
    }

    @Test
    @DisplayName("Parse DISTINCT ON with WHERE and ORDER BY")
    void parsesDistinctOnWithWhereAndOrder() {
        var qResult = parseQuery("""
            SELECT DISTINCT ON (user_id) *
            FROM orders
            WHERE status = 'active'
            ORDER BY user_id, created_at DESC
            """);
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        
        assertNotNull(query.distinct());
        assertNotNull(query.where());
        assertNotNull(query.orderBy());
    }

    @Test
    @DisplayName("Reject DISTINCT ON without opening parenthesis")
    void rejectsDistinctOnMissingOpenParen() {
        assertParseError("SELECT DISTINCT ON user_id * FROM orders");
    }

    @Test
    @DisplayName("Reject DISTINCT ON without closing parenthesis")
    void rejectsDistinctOnMissingCloseParen() {
        assertParseError("SELECT DISTINCT ON (user_id * FROM orders");
    }

    @Test
    @DisplayName("Reject DISTINCT ON with empty expression list")
    void rejectsDistinctOnEmptyList() {
        assertParseError("SELECT DISTINCT ON () * FROM orders");
    }

    private ParseResult<? extends Query> parseQuery(String sql) {
        return parseContext.parse(Query.class, sql);
    }

    private void assertParseError(String sql) {
        var result = parseQuery(sql);
        if (result.ok()) {
            fail("Expected parse error for: " + sql);
        }
    }
}
