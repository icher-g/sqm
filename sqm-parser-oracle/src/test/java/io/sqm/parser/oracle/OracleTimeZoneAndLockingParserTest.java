package io.sqm.parser.oracle;

import io.sqm.core.AtTimeZoneExpr;
import io.sqm.core.ExprSelectItem;
import io.sqm.core.LockWaitMode;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.parser.oracle.spi.OracleSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OracleTimeZoneAndLockingParserTest {
    private final ParseContext ctx = ParseContext.of(new OracleSpecs());

    @Test
    void parsesAtTimeZoneExpression() {
        var result = ctx.parse(Query.class, "SELECT created_at AT TIME ZONE 'UTC' FROM users");

        assertTrue(result.ok(), result.errorMessage());
        var query = assertInstanceOf(SelectQuery.class, result.value());
        var item = assertInstanceOf(ExprSelectItem.class, query.items().getFirst());
        var expr = assertInstanceOf(AtTimeZoneExpr.class, item.expr());
        assertEquals("UTC", expr.timezone().matchExpression().literal(l -> l.value()).orElseThrow(AssertionError::new));
    }

    @Test
    void parsesForUpdateOfWait() {
        var result = ctx.parse(Query.class, "SELECT * FROM users u FOR UPDATE OF u WAIT 5");

        assertTrue(result.ok(), result.errorMessage());
        var query = assertInstanceOf(SelectQuery.class, result.value());
        assertNotNull(query.lockFor());
        assertEquals("u", query.lockFor().ofTables().getFirst().identifier().value());
        assertEquals(LockWaitMode.WAIT, query.lockFor().waitMode());
        assertEquals(5L, query.lockFor().waitSeconds().matchExpression().literal(l -> l.value()).orElseThrow(AssertionError::new));
    }

    @Test
    void rejectsMalformedWait() {
        assertTrue(ctx.parse(Query.class, "SELECT * FROM users FOR UPDATE WAIT").isError());
    }
}
