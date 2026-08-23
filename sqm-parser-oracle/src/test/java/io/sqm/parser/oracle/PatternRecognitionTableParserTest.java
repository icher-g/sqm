package io.sqm.parser.oracle;

import io.sqm.core.PatternRecognitionTable;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.parser.oracle.spi.OracleSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatternRecognitionTableParserTest {
    private static final String MINIMAL = """
        SELECT * FROM sales MATCH_RECOGNIZE (
          PATTERN (A)
          DEFINE A AS A.amount > 0
        ) mr
        """;

    @Test
    void parsesSharedGrammarForOracle12AndLater() {
        for (var version : new SqlDialectVersion[]{SqlDialectVersion.of(12, 1), SqlDialectVersion.of(19, 0)}) {
            var result = ParseContext.of(new OracleSpecs(version)).parse(Query.class, MINIMAL);

            assertTrue(result.ok(), result.errorMessage());
            assertInstanceOf(PatternRecognitionTable.class,
                assertInstanceOf(SelectQuery.class, result.value()).from());
        }
    }

    @Test
    void rejectsMatchRecognizeBeforeOracle12_1() {
        var result = ParseContext.of(new OracleSpecs(SqlDialectVersion.of(11, 2))).parse(Query.class, MINIMAL);

        assertTrue(result.isError());
        assertTrue(result.errorMessage().contains("MATCH_RECOGNIZE is not supported"));
    }

    @Test
    void rejectsNonOracleRowsPerMatchOptions() {
        var result = ParseContext.of(new OracleSpecs()).parse(Query.class, """
            SELECT * FROM sales MATCH_RECOGNIZE (
              ALL ROWS PER MATCH WITH UNMATCHED ROWS
              PATTERN (A)
              DEFINE A AS A.amount > 0
            )
            """);

        assertTrue(result.isError());
        assertTrue(result.errorMessage().contains("empty or unmatched-row handling"));
    }
}
