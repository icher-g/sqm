package io.sqm.parser.mysql;

import io.sqm.core.GroupBy;
import io.sqm.core.LimitOffset;
import io.sqm.core.RegexPredicate;
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

        assertInstanceOf(io.sqm.parser.ansi.GroupByParser.class, ansi.require(GroupBy.class));
        assertInstanceOf(MySqlGroupByParser.class, mysql.require(GroupBy.class));

        assertInstanceOf(io.sqm.parser.ansi.RegexPredicateParser.class, ansi.require(RegexPredicate.class));
        assertInstanceOf(MySqlRegexPredicateParser.class, mysql.require(RegexPredicate.class));
    }
}
