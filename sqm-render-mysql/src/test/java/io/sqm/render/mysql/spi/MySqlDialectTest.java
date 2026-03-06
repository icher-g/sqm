package io.sqm.render.mysql.spi;

import io.sqm.core.QuoteStyle;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.render.ansi.spi.AnsiBooleans;
import io.sqm.render.ansi.spi.AnsiNullSorting;
import io.sqm.render.defaults.DefaultOperators;
import io.sqm.render.defaults.DefaultValueFormatter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void constructors_with_version_and_ansi_mode_are_usable() {
        var versioned = new MySqlDialect(SqlDialectVersion.of(8, 0));
        var ansiQuotes = new MySqlDialect(SqlDialectVersion.of(8, 0), true);

        assertEquals("MySQL", versioned.name());
        assertTrue(ansiQuotes.quoter().supports(QuoteStyle.DOUBLE_QUOTE));
    }
}

