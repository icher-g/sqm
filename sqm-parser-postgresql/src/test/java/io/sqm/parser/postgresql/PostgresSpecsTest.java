package io.sqm.parser.postgresql;

import io.sqm.parser.postgresql.spi.PostgresSpecs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PostgresSpecsTest {

    @Test
    void parsers_returnsRepository() {
        var specs = new PostgresSpecs();
        assertNotNull(specs.parsers());
    }

    @Test
    void lookups_isLazilyInitializedAndCached() {
        var specs = new PostgresSpecs();
        var first = specs.lookups();
        var second = specs.lookups();

        assertNotNull(first);
        assertSame(first, second);
    }

    @Test
    void identifierQuoting_isLazilyInitializedAndCached() {
        var specs = new PostgresSpecs();
        var first = specs.identifierQuoting();
        var second = specs.identifierQuoting();

        assertNotNull(first);
        assertSame(first, second);
    }
}
