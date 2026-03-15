package io.sqm.parser.sqlserver.spi;

import io.sqm.core.Query;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlServerSpecsTest {

    @Test
    void defaults_to_bracket_identifier_quoting() {
        var specs = new SqlServerSpecs();

        assertTrue(specs.identifierQuoting().supports('['));
        assertFalse(specs.identifierQuoting().supports('"'));
    }

    @Test
    void supports_double_quotes_when_quoted_identifier_mode_is_enabled() {
        var specs = new SqlServerSpecs(SqlDialectVersion.of(2019, 0), true);

        assertTrue(specs.identifierQuoting().supports('['));
        assertTrue(specs.identifierQuoting().supports('"'));
    }

    @Test
    void parses_simple_query_with_bracket_identifiers() {
        var context = ParseContext.of(new SqlServerSpecs());
        var result = context.parse(Query.class, "SELECT [u].[id] FROM [users] AS [u]");

        assertFalse(result.isError());
        assertInstanceOf(Query.class, result.value());
    }
}
