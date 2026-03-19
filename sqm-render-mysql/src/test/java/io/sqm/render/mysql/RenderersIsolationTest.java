package io.sqm.render.mysql;

import io.sqm.core.GroupBy;
import io.sqm.core.InsertStatement;
import io.sqm.core.LimitOffset;
import io.sqm.core.RegexPredicate;
import io.sqm.core.SelectQuery;
import io.sqm.core.Table;
import io.sqm.render.spi.RenderersRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class RenderersIsolationTest {

    @Test
    void mysqlRegistry_doesNotMutateAnsiRegistry() {
        RenderersRepository ansi = io.sqm.render.ansi.Renderers.ansi();
        RenderersRepository mysql = Renderers.mysql();

        assertInstanceOf(io.sqm.render.ansi.InsertStatementRenderer.class, ansi.require(InsertStatement.class));
        assertInstanceOf(InsertStatementRenderer.class, mysql.require(InsertStatement.class));

        assertInstanceOf(io.sqm.render.ansi.LimitOffsetRenderer.class, ansi.require(LimitOffset.class));
        assertInstanceOf(LimitOffsetRenderer.class, mysql.require(LimitOffset.class));

        assertInstanceOf(io.sqm.render.ansi.GroupByRenderer.class, ansi.require(GroupBy.class));
        assertInstanceOf(GroupByRenderer.class, mysql.require(GroupBy.class));

        assertInstanceOf(io.sqm.render.ansi.RegexPredicateRenderer.class, ansi.require(RegexPredicate.class));
        assertInstanceOf(RegexPredicateRenderer.class, mysql.require(RegexPredicate.class));

        assertInstanceOf(io.sqm.render.ansi.TableRenderer.class, ansi.require(Table.class));
        assertInstanceOf(TableRenderer.class, mysql.require(Table.class));

        assertInstanceOf(io.sqm.render.ansi.SelectQueryRenderer.class, ansi.require(SelectQuery.class));
        assertInstanceOf(SelectQueryRenderer.class, mysql.require(SelectQuery.class));
    }
}
