package io.sqm.render.oracle;

import io.sqm.core.RowsPerMatch;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.oracle.spi.OracleDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PatternRecognitionTableRendererTest {
    @Test
    void rendersOracleAliasesAndMeasureAliasesLegally() {
        var table = matchRecognize(tbl("sales"))
            .measure(matchNumber(), "match_no")
            .pattern(patternVar("A"))
            .define("A", patternColumn("A", "amount").gt(0))
            .as("mr")
            .build();

        assertEquals(
            "sales MATCH_RECOGNIZE ( MEASURES MATCH_NUMBER() AS match_no ONE ROW PER MATCH "
                + "AFTER MATCH SKIP PAST LAST ROW PATTERN (A) DEFINE A AS A.amount > 0 ) mr",
            normalize(RenderContext.of(new OracleDialect()).render(table).sql())
        );
    }

    @Test
    void rejectsOracle11BeforeWritingMatchRecognizeSql() {
        var table = matchRecognize(tbl("sales"))
            .pattern(patternVar("A"))
            .define("A", patternColumn("A", "amount").gt(0))
            .build();

        assertThrows(UnsupportedDialectFeatureException.class,
            () -> RenderContext.of(new OracleDialect(SqlDialectVersion.of(11, 2))).render(table));
    }

    @Test
    void rejectsNonOracleRowsPerMatchOptions() {
        var table = matchRecognize(tbl("sales"))
            .rowsPerMatch(allRowsPerMatch(RowsPerMatch.EmptyMatchHandling.WITH_UNMATCHED))
            .pattern(patternVar("A"))
            .define("A", patternColumn("A", "amount").gt(0))
            .build();

        assertThrows(UnsupportedDialectFeatureException.class,
            () -> RenderContext.of(new OracleDialect()).render(table));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
