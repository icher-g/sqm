package io.sqm.parser.mysql;

import io.sqm.core.LimitOffset;
import io.sqm.parser.spi.ParsersRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ParsersIsolationTest {

    @Test
    void mysqlRegistry_doesNotMutateAnsiRegistry() {
        ParsersRepository ansi = io.sqm.parser.ansi.Parsers.ansi();
        ParsersRepository mysql = Parsers.mysql();

        assertInstanceOf(io.sqm.parser.ansi.LimitOffsetParser.class, ansi.require(LimitOffset.class));
        assertInstanceOf(MySqlLimitOffsetParser.class, mysql.require(LimitOffset.class));
    }
}

