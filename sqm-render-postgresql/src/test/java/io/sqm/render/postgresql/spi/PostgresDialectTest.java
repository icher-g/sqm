package io.sqm.render.postgresql.spi;

import io.sqm.render.spi.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PostgresDialectTest {

    private final PostgresDialect dialect = new PostgresDialect();

    @Test
    void exposesDialectComponents() {
        assertEquals("PostgreSQL", dialect.name());
        assertInstanceOf(PostgresIdentifierQuoter.class, dialect.quoter());
        assertInstanceOf(PostgresValueFormatter.class, dialect.formatter());
        assertInstanceOf(PostgresOperators.class, dialect.operators());
        assertInstanceOf(PostgresBooleans.class, dialect.booleans());
        assertInstanceOf(PostgresNullSorting.class, dialect.nullSorting());
        assertInstanceOf(PostgresPaginationStyle.class, dialect.paginationStyle());
        assertNotNull(dialect.renderers());
    }
}
