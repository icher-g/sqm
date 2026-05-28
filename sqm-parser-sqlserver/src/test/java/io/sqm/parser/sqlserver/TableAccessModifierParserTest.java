package io.sqm.parser.sqlserver;

import io.sqm.core.SampledTable;
import io.sqm.core.SelectQuery;
import io.sqm.core.Table;
import io.sqm.core.TableSampleSpec;
import io.sqm.core.TableVersionSpec;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableAccessModifierParserTest {
    private final ParseContext ctx = ParseContext.of(new SqlServerSpecs());

    @Test
    void parsesSystemTimeAsOf() {
        var result = ctx.parse(SelectQuery.class, "SELECT * FROM orders FOR SYSTEM_TIME AS OF 42 o");

        assertTrue(result.ok(), result.errorMessage());
        var table = assertInstanceOf(Table.class, result.value().from());
        assertEquals(TableVersionSpec.TableVersionKind.AS_OF_TIMESTAMP, table.version().kind());
        assertEquals("o", table.alias().value());
    }

    @Test
    void parsesSystemTimeRangeSelectors() {
        var fromTo = ctx.parse(SelectQuery.class, "SELECT * FROM orders FOR SYSTEM_TIME FROM 10 TO 20");
        var between = ctx.parse(SelectQuery.class, "SELECT * FROM orders FOR SYSTEM_TIME BETWEEN 10 AND 20");
        var contained = ctx.parse(SelectQuery.class, "SELECT * FROM orders FOR SYSTEM_TIME CONTAINED IN (10, 20)");
        var all = ctx.parse(SelectQuery.class, "SELECT * FROM orders FOR SYSTEM_TIME ALL");

        assertTrue(fromTo.ok(), fromTo.errorMessage());
        assertTrue(between.ok(), between.errorMessage());
        assertTrue(contained.ok(), contained.errorMessage());
        assertTrue(all.ok(), all.errorMessage());
        assertEquals(TableVersionSpec.TableVersionKind.FROM_TO, assertInstanceOf(Table.class, fromTo.value().from()).version().kind());
        assertEquals(TableVersionSpec.TableVersionKind.BETWEEN, assertInstanceOf(Table.class, between.value().from()).version().kind());
        assertEquals(TableVersionSpec.TableVersionKind.CONTAINED_IN, assertInstanceOf(Table.class, contained.value().from()).version().kind());
        assertEquals(TableVersionSpec.TableVersionKind.ALL, assertInstanceOf(Table.class, all.value().from()).version().kind());
    }

    @Test
    void parsesTableSampleRows() {
        var result = ctx.parse(SelectQuery.class, "SELECT * FROM users TABLESAMPLE (100 ROWS) REPEATABLE (42) u");

        assertTrue(result.ok(), result.errorMessage());
        var sampled = assertInstanceOf(SampledTable.class, result.value().from());
        assertEquals(TableSampleSpec.SampleUnit.ROWS, sampled.sampleSpec().unit());
    }

    @Test
    void parsesTableSamplePercent() {
        var result = ctx.parse(SelectQuery.class, "SELECT * FROM users TABLESAMPLE (10 PERCENT)");

        assertTrue(result.ok(), result.errorMessage());
        var sampled = assertInstanceOf(SampledTable.class, result.value().from());
        assertEquals(TableSampleSpec.SampleMethod.SYSTEM, sampled.sampleSpec().method());
        assertEquals(TableSampleSpec.SampleUnit.PERCENT, sampled.sampleSpec().unit());
    }

    @Test
    void rejectsIncompleteSystemTimeSyntax() {
        var result = ctx.parse(SelectQuery.class, "SELECT * FROM orders FOR SYSTEM_TIME CONTAINED (10, 20)");

        assertFalse(result.ok());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Expected SYSTEM_TIME selector")
            || Objects.requireNonNull(result.errorMessage()).contains("Expected IN"));
    }
}
