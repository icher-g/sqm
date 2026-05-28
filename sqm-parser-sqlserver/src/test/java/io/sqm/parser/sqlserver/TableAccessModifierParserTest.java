package io.sqm.parser.sqlserver;

import io.sqm.core.SampledTable;
import io.sqm.core.SelectQuery;
import io.sqm.core.Table;
import io.sqm.core.TableSampleSpec;
import io.sqm.core.TableVersionSpec;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void parsesTableSampleRows() {
        var result = ctx.parse(SelectQuery.class, "SELECT * FROM users TABLESAMPLE (100 ROWS) REPEATABLE (42) u");

        assertTrue(result.ok(), result.errorMessage());
        var sampled = assertInstanceOf(SampledTable.class, result.value().from());
        assertEquals(TableSampleSpec.SampleUnit.ROWS, sampled.sampleSpec().unit());
    }
}
