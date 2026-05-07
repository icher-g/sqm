package io.sqm.parser.oracle.spi;

import io.sqm.core.Query;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OracleSpecsTest {

    @Test
    void supports_double_quoted_identifiers() {
        var specs = new OracleSpecs();

        assertTrue(specs.identifierQuoting().supports('"'));
        assertFalse(specs.identifierQuoting().supports('['));
        assertFalse(specs.identifierQuoting().supports('`'));
    }

    @Test
    void exposes_oracle_capabilities_for_configured_version() {
        var specs = new OracleSpecs(SqlDialectVersion.of(11, 2));

        assertTrue(specs.capabilities().supports(SqlFeature.MERGE_STATEMENT));
        assertFalse(specs.capabilities().supports(SqlFeature.LATERAL));
    }

    @Test
    void parses_simple_query_with_oracle_specs() {
        var context = ParseContext.of(new OracleSpecs());
        var result = context.parse(Query.class, "SELECT \"u\".\"id\" FROM \"users\" AS \"u\"");

        assertFalse(result.isError());
        assertInstanceOf(Query.class, result.value());
    }
}
