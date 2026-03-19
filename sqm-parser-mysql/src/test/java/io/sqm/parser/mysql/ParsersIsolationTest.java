package io.sqm.parser.mysql;

import io.sqm.core.GroupBy;
import io.sqm.core.InsertStatement;
import io.sqm.core.LimitOffset;
import io.sqm.core.RegexPredicate;
import io.sqm.core.SelectQuery;
import io.sqm.core.Table;
import io.sqm.parser.spi.ParsersRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ParsersIsolationTest {

    @Test
    void mysqlRegistry_doesNotMutateAnsiRegistry() {
        ParsersRepository ansi = io.sqm.parser.ansi.Parsers.ansi();
        ParsersRepository mysql = Parsers.mysql();

        assertInstanceOf(io.sqm.parser.ansi.InsertStatementParser.class, ansi.require(InsertStatement.class));
        assertInstanceOf(InsertStatementParser.class, mysql.require(InsertStatement.class));

        assertInstanceOf(io.sqm.parser.ansi.LimitOffsetParser.class, ansi.require(LimitOffset.class));
        assertInstanceOf(LimitOffsetParser.class, mysql.require(LimitOffset.class));

        assertInstanceOf(io.sqm.parser.ansi.GroupByParser.class, ansi.require(GroupBy.class));
        assertInstanceOf(GroupByParser.class, mysql.require(GroupBy.class));

        assertInstanceOf(io.sqm.parser.ansi.RegexPredicateParser.class, ansi.require(RegexPredicate.class));
        assertInstanceOf(RegexPredicateParser.class, mysql.require(RegexPredicate.class));

        assertInstanceOf(io.sqm.parser.ansi.TableParser.class, ansi.require(Table.class));
        assertInstanceOf(TableParser.class, mysql.require(Table.class));

        assertInstanceOf(io.sqm.parser.ansi.SelectQueryParser.class, ansi.require(SelectQuery.class));
        assertInstanceOf(SelectQueryParser.class, mysql.require(SelectQuery.class));
    }
}
