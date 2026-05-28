package io.sqm.parser.ansi;

import io.sqm.core.SampledTable;
import io.sqm.core.TableSampleSpec;
import io.sqm.core.TableRef;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.*;

class SampledTableParserTest {
    @Test
    void standaloneParseReportsMissingSource() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var parser = new SampledTableParser();

        var result = parser.parse(Cursor.of("TABLESAMPLE SYSTEM (10)", ctx.identifierQuoting()), ctx);

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("requires a source table"));
        assertEquals(SampledTable.class, parser.targetType());
    }

    @Test
    void matchesOnlyTableSampleKeyword() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var parser = new SampledTableParser();

        assertTrue(parser.match(Cursor.of("TABLESAMPLE SYSTEM (10)", ctx.identifierQuoting()), ctx));
        assertFalse(parser.match(Cursor.of("SAMPLE (10)", ctx.identifierQuoting()), ctx));
    }

    @Test
    void parsesTableSampleSystemPercentWithRepeatableSeedAndAlias() {
        var ctx = ParseContext.of(new TestSpecs());

        var result = ctx.parse(
            SampledTable.class,
            tbl("users"),
            Cursor.of("TABLESAMPLE SYSTEM (10 PERCENT) REPEATABLE (42) sampled_users", ctx.identifierQuoting()));

        assertTrue(result.ok(), () -> "problems: " + result.problems());
        var sampled = result.value();
        assertEquals("sampled_users", sampled.alias().value());
        assertEquals(TableSampleSpec.SampleMethod.SYSTEM, sampled.sampleSpec().method());
        assertEquals(TableSampleSpec.SampleUnit.PERCENT, sampled.sampleSpec().unit());
        assertEquals(10L, sampled.sampleSpec().amount().matchExpression().literal(l -> l.value()).orElseThrow(AssertionError::new));
        assertEquals(42L, sampled.sampleSpec().repeatableSeed().matchExpression().literal(l -> l.value()).orElseThrow(AssertionError::new));
    }

    @Test
    void parsesTableSampleBernoulliRows() {
        var ctx = ParseContext.of(new TestSpecs());

        var result = ctx.parse(
            SampledTable.class,
            tbl("users"),
            Cursor.of("TABLESAMPLE BERNOULLI (5 ROWS)", ctx.identifierQuoting()));

        assertTrue(result.ok(), () -> "problems: " + result.problems());
        var sample = result.value().sampleSpec();
        assertEquals(TableSampleSpec.SampleMethod.BERNOULLI, sample.method());
        assertEquals(TableSampleSpec.SampleUnit.ROWS, sample.unit());
        assertNull(sample.repeatableSeed());
    }

    @Test
    void parsesOmittedMethodAsSystemWithUnspecifiedUnit() {
        var ctx = ParseContext.of(new TestSpecs());

        var result = ctx.parse(
            SampledTable.class,
            tbl("users"),
            Cursor.of("TABLESAMPLE (25)", ctx.identifierQuoting()));

        assertTrue(result.ok(), () -> "problems: " + result.problems());
        var sample = result.value().sampleSpec();
        assertEquals(TableSampleSpec.SampleMethod.SYSTEM, sample.method());
        assertEquals(TableSampleSpec.SampleUnit.UNSPECIFIED, sample.unit());
    }

    @Test
    void reportsExpressionErrorsFromAmountAndSeed() {
        var ctx = ParseContext.of(new TestSpecs());
        TableRef source = tbl("users");

        assertTrue(ctx.parse(SampledTable.class, source, Cursor.of("TABLESAMPLE SYSTEM ()", ctx.identifierQuoting())).isError());
        assertTrue(ctx.parse(SampledTable.class, source, Cursor.of("TABLESAMPLE SYSTEM (10) REPEATABLE ()", ctx.identifierQuoting())).isError());
    }

    @Test
    void rejectsTableSampleWhenDialectCapabilityIsDisabled() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var parser = new SampledTableParser();

        var result = parser.parse(tbl("users"), Cursor.of("TABLESAMPLE SYSTEM (10)", ctx.identifierQuoting()), ctx);

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("not supported"));
    }
}
