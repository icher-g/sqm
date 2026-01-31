package io.sqm.parser.postgresql;

import io.sqm.core.*;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parser tests for PostgreSQL {@link FunctionTable}.
 *
 * <p>Tests table-valued functions in FROM clause.</p>
 */
@DisplayName("PostgreSQL FunctionTableParser Tests")
class FunctionTableParserTest {

    private final ParseContext parseContext = ParseContext.of(new PostgresSpecs());

    @Test
    @DisplayName("Parse simple table function")
    void parsesSimpleTableFunction() {
        var qResult = parseQuery("SELECT * FROM generate_series(1, 10)");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        
        assertNotNull(query.from());
        var table = assertInstanceOf(FunctionTable.class, query.from());
        assertInstanceOf(FunctionExpr.class, table.function());
    }

    @Test
    @DisplayName("Parse table function with alias")
    void parsesTableFunctionWithAlias() {
        var qResult = parseQuery("SELECT * FROM generate_series(1, 10) AS nums");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        
        var table = assertInstanceOf(FunctionTable.class, query.from());
        assertNotNull(table.alias());
        assertEquals("nums", table.alias());
    }

    @Test
    @DisplayName("Parse table function with column aliases")
    void parsesTableFunctionWithColumnAliases() {
        var qResult = parseQuery("SELECT * FROM generate_series(1, 10) AS t(n)");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        
        var table = assertInstanceOf(FunctionTable.class, query.from());
        assertNotNull(table.alias());
        assertEquals("t", table.alias());
        assertEquals(1, table.columnAliases().size());
        assertEquals("n", table.columnAliases().getFirst());
    }

    @Test
    @DisplayName("Parse table function WITH ORDINALITY")
    void parsesTableFunctionWithOrdinality() {
        var qResult = parseQuery("SELECT * FROM generate_series(1, 3) WITH ORDINALITY AS t(n, ord)");

        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());

        var table = assertInstanceOf(FunctionTable.class, query.from());
        assertTrue(table.ordinality());
        assertEquals("t", table.alias());
        assertEquals(2, table.columnAliases().size());
    }

    @Test
    @DisplayName("Parse table function WITH ORDINALITY without alias")
    void parsesTableFunctionWithOrdinalityWithoutAlias() {
        var qResult = parseQuery("SELECT * FROM generate_series(1, 3) WITH ORDINALITY");

        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());

        var table = assertInstanceOf(FunctionTable.class, query.from());
        assertTrue(table.ordinality());
        assertNull(table.alias());
    }

    @Test
    @DisplayName("Parse table function with implicit alias and column aliases")
    void parsesTableFunctionWithImplicitAliasAndColumns() {
        var qResult = parseQuery("SELECT * FROM generate_series(1, 10) t(n)");

        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());

        var table = assertInstanceOf(FunctionTable.class, query.from());
        assertEquals("t", table.alias());
        assertEquals(1, table.columnAliases().size());
        assertEquals("n", table.columnAliases().getFirst());
    }

    @Test
    @DisplayName("Parse unnest function")
    void parsesUnnestFunction() {
        var qResult = parseQuery("SELECT * FROM unnest(ARRAY[1,2,3])");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        
        var table = assertInstanceOf(FunctionTable.class, query.from());
        var func = table.function();
        assertEquals("unnest", func.name());
    }

    @Test
    @DisplayName("Parse json_array_elements function")
    void parsesJsonArrayElementsFunction() {
        var qResult = parseQuery("SELECT * FROM json_array_elements('[1,2,3]'::json)");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        
        var table = assertInstanceOf(FunctionTable.class, query.from());
        assertEquals("json_array_elements", table.function().name());
    }

    @Test
    @DisplayName("Parse table function in JOIN")
    void parsesTableFunctionInJoin() {
        var qResult = parseQuery("SELECT * FROM users u JOIN generate_series(1, 10) s(n) ON u.id = s.n");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        
        assertEquals(1, query.joins().size());
        var join = assertInstanceOf(OnJoin.class, query.joins().getFirst());
        assertInstanceOf(FunctionTable.class, join.right());
    }

    @Test
    @DisplayName("Parse multiple table functions")
    void parsesMultipleTableFunctions() {
        var qResult = parseQuery("SELECT * FROM generate_series(1, 5) s1, generate_series(1, 3) s2");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        
        assertInstanceOf(FunctionTable.class, query.from());
        assertEquals(1, query.joins().size());
    }

    @Test
    @DisplayName("Parse table function with WHERE clause")
    void parsesTableFunctionWithWhere() {
        var qResult = parseQuery("SELECT * FROM generate_series(1, 100) s(n) WHERE n % 2 = 0");
        
        assertTrue(qResult.ok());
        var query = assertInstanceOf(SelectQuery.class, qResult.value());
        
        assertNotNull(query.where());
    }

    @Test
    @DisplayName("Reject table function with missing closing parenthesis")
    void rejectsTableFunctionMissingClosingParen() {
        assertParseError("SELECT * FROM generate_series(1, 10");
    }

    @Test
    @DisplayName("Reject WITH without ORDINALITY")
    void rejectsWithWithoutOrdinality() {
        assertParseError("SELECT * FROM generate_series(1, 3) WITH t");
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
