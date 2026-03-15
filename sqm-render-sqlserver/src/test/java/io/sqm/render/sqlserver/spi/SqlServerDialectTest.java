package io.sqm.render.sqlserver.spi;

import io.sqm.render.defaults.DefaultOperators;
import io.sqm.render.defaults.DefaultValueFormatter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SqlServerDialectTest {

    private final SqlServerDialect dialect = new SqlServerDialect();

    @Test
    void exposesDialectComponents() {
        assertEquals("SQL Server", dialect.name());
        assertInstanceOf(SqlServerIdentifierQuoter.class, dialect.quoter());
        assertInstanceOf(DefaultValueFormatter.class, dialect.formatter());
        assertInstanceOf(DefaultOperators.class, dialect.operators());
        assertInstanceOf(SqlServerBooleans.class, dialect.booleans());
        assertInstanceOf(SqlServerPaginationStyle.class, dialect.paginationStyle());
        assertNotNull(dialect.nullSorting());
        assertNotNull(dialect.renderers());
    }
}
