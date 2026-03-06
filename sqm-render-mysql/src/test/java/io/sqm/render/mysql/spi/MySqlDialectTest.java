package io.sqm.render.mysql.spi;

import io.sqm.render.ansi.spi.AnsiBooleans;
import io.sqm.render.ansi.spi.AnsiNullSorting;
import io.sqm.render.defaults.DefaultOperators;
import io.sqm.render.defaults.DefaultValueFormatter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MySqlDialectTest {

    private final MySqlDialect dialect = new MySqlDialect();

    @Test
    void exposesDialectComponents() {
        assertEquals("MySQL", dialect.name());
        assertInstanceOf(MySqlIdentifierQuoter.class, dialect.quoter());
        assertInstanceOf(DefaultValueFormatter.class, dialect.formatter());
        assertInstanceOf(DefaultOperators.class, dialect.operators());
        assertInstanceOf(AnsiBooleans.class, dialect.booleans());
        assertInstanceOf(AnsiNullSorting.class, dialect.nullSorting());
        assertInstanceOf(MySqlPaginationStyle.class, dialect.paginationStyle());
        assertNotNull(dialect.renderers());
        assertNotNull(dialect.capabilities());
    }
}
