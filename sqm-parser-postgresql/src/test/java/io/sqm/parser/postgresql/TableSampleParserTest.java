package io.sqm.parser.postgresql;

import io.sqm.core.SampledTable;
import io.sqm.core.SelectQuery;
import io.sqm.core.TableSampleSpec;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableSampleParserTest {
    private final ParseContext ctx = ParseContext.of(new PostgresSpecs());

    @Test
    void parsesBernoulliTableSample() {
        var result = ctx.parse(SelectQuery.class, "SELECT * FROM users TABLESAMPLE BERNOULLI (10) REPEATABLE (42) u");

        assertTrue(result.ok(), result.errorMessage());
        var sampled = assertInstanceOf(SampledTable.class, result.value().from());
        assertEquals(TableSampleSpec.SampleMethod.BERNOULLI, sampled.sampleSpec().method());
        assertEquals("u", sampled.alias().value());
    }
}
