package io.sqm.parser.pgsql;

import io.sqm.core.*;
import io.sqm.parser.pgsql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parser tests for PostgreSQL {@link Lateral}.
 *
 * <p>Tests LATERAL keyword for correlated subqueries in FROM clause.</p>
 */
@DisplayName("PostgreSQL LateralParser Tests")
class LateralParserTest {

    private final ParseContext parseContext = ParseContext.of(new PostgresSpecs());

    @Test
    @DisplayName("Parse LATERAL with subquery")
    void parsesLateralWithSubquery() {
        var qResult = parseQuery("SELECT * FROM users u, LATERAL (SELECT * FROM orders WHERE user_id = u.id) o");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        
        // Check that we have 2 FROM items
        assertNotNull(query.from());
        assertEquals(1, query.joins().size());
    }

    @Test
    @DisplayName("Parse LATERAL with function call")
    void parsesLateralWithFunction() {
        var qResult = parseQuery("SELECT * FROM users u, LATERAL unnest(u.tags) AS t(tag)");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        
        assertNotNull(query.from());
        assertEquals(1, query.joins().size());
    }

    @Test
    @DisplayName("Parse LATERAL in JOIN")
    void parsesLateralInJoin() {
        var qResult = parseQuery("SELECT * FROM users u JOIN LATERAL (SELECT * FROM orders WHERE user_id = u.id) o ON true");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        
        assertEquals(1, query.joins().size());
    }

    @Test
    @DisplayName("Parse LATERAL with VALUES")
    void parsesLateralWithValues() {
        var qResult = parseQuery("SELECT * FROM users u, LATERAL (VALUES (u.id, u.name)) v(id, name)");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        
        assertNotNull(query.from());
    }

    @Test
    @DisplayName("Parse multiple LATERAL joins")
    void parsesMultipleLateralJoins() {
        var qResult = parseQuery("""
            SELECT * FROM users u
            JOIN LATERAL (SELECT * FROM orders WHERE user_id = u.id) o ON true
            JOIN LATERAL (SELECT * FROM payments WHERE order_id = o.id) p ON true
            """);
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        
        assertEquals(2, query.joins().size());
    }

    @Test
    @DisplayName("Parse LATERAL LEFT JOIN")
    void parsesLateralLeftJoin() {
        var qResult = parseQuery("SELECT * FROM users u LEFT JOIN LATERAL (SELECT * FROM orders WHERE user_id = u.id LIMIT 1) o ON true");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        
        assertEquals(1, query.joins().size());
        var join = assertInstanceOf(OnJoin.class, query.joins().getFirst());
        assertEquals(JoinKind.LEFT, join.kind());
    }

    @Test
    @DisplayName("Parse LATERAL with aggregate")
    void parsesLateralWithAggregate() {
        var qResult = parseQuery("SELECT * FROM users u, LATERAL (SELECT COUNT(*) AS order_count FROM orders WHERE user_id = u.id) cnt");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        
        assertNotNull(query.from());
    }

    private ParseResult<? extends Query> parseQuery(String sql) {
        return parseContext.parse(Query.class, sql);
    }
}
