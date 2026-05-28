package io.sqm.parser.oracle;

import io.sqm.core.SampledTable;
import io.sqm.core.SelectQuery;
import io.sqm.core.Table;
import io.sqm.core.TablePartitionSpec;
import io.sqm.core.TableSampleSpec;
import io.sqm.core.TableVersionSpec;
import io.sqm.parser.oracle.spi.OracleSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class TableAccessModifierParserTest {
    private final ParseContext ctx = ParseContext.of(new OracleSpecs());

    @Test
    void parsesFlashbackTimestamp() {
        var result = ctx.parse(SelectQuery.class, "SELECT * FROM orders AS OF TIMESTAMP 42");

        assertTrue(result.ok(), result.errorMessage());
        var table = assertInstanceOf(Table.class, result.value().from());
        assertEquals(TableVersionSpec.TableVersionKind.AS_OF_TIMESTAMP, table.version().kind());
    }

    @Test
    void parsesFlashbackScnAndPartition() {
        var result = ctx.parse(SelectQuery.class, "SELECT * FROM sales AS OF SCN 42 PARTITION (sales_q1)");

        assertTrue(result.ok(), result.errorMessage());
        var table = assertInstanceOf(Table.class, result.value().from());
        assertEquals(TableVersionSpec.TableVersionKind.AS_OF_SCN, table.version().kind());
        assertEquals(TablePartitionSpec.TablePartitionSpecKind.PARTITION, table.partitionSpec().kind());
        assertEquals("sales_q1", table.partitionSpec().names().getFirst().value());
    }

    @Test
    void parsesSubpartitionList() {
        var result = ctx.parse(SelectQuery.class, "SELECT * FROM sales SUBPARTITION (sales_q1_eu, sales_q1_us) s");

        assertTrue(result.ok(), result.errorMessage());
        var table = assertInstanceOf(Table.class, result.value().from());
        assertEquals(TablePartitionSpec.TablePartitionSpecKind.SUBPARTITION, table.partitionSpec().kind());
        assertEquals("sales_q1_us", table.partitionSpec().names().get(1).value());
        assertEquals("s", table.alias().value());
    }

    @Test
    void parsesSampleBlockSeed() {
        var result = ctx.parse(SelectQuery.class, "SELECT * FROM users SAMPLE BLOCK (10) SEED (42) u");

        assertTrue(result.ok(), result.errorMessage());
        var sampled = assertInstanceOf(SampledTable.class, result.value().from());
        assertEquals(TableSampleSpec.SampleMethod.BLOCK, sampled.sampleSpec().method());
        assertEquals(TableSampleSpec.SampleUnit.PERCENT, sampled.sampleSpec().unit());
        assertEquals("u", sampled.alias().value());
    }

    @Test
    void parsesRowSampleWithoutSeed() {
        var result = ctx.parse(SelectQuery.class, "SELECT * FROM users SAMPLE (10)");

        assertTrue(result.ok(), result.errorMessage());
        var sampled = assertInstanceOf(SampledTable.class, result.value().from());
        assertEquals(TableSampleSpec.SampleMethod.DIALECT_DEFAULT, sampled.sampleSpec().method());
        assertEquals(TableSampleSpec.SampleUnit.PERCENT, sampled.sampleSpec().unit());
        assertNull(sampled.sampleSpec().repeatableSeed());
    }

    @Test
    void rejectsIncompleteFlashbackSyntax() {
        var result = ctx.parse(SelectQuery.class, "SELECT * FROM orders AS OF 42");

        assertFalse(result.ok());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Expected SCN or TIMESTAMP"));
    }
}
