package io.sqm.it;

import io.sqm.core.Query;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.VersionedDialectCapabilities;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.mysql.spi.MySqlDialect;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.sqlserver.spi.SqlServerDialect;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Integration coverage for shared {@code MATCH_RECOGNIZE} parse/render symmetry. */
class MatchRecognizeRoundTripIntegrationTest {
    private final ParseContext parser = ParseContext.of(new PatternSpecs());
    private final RenderContext renderer = RenderContext.of(new PatternDialect());

    @Test
    void roundTripsCompletePatternRecognitionSemantically() {
        var parsed = parser.parse(Query.class, """
            SELECT *
            FROM sales MATCH_RECOGNIZE (
              PARTITION BY customer_id
              ORDER BY sale_date
              MEASURES MATCH_NUMBER() AS match_no,
                       FIRST(A.amount) AS first_amount,
                       FINAL LAST(B.amount, 1) AS last_amount
              ALL ROWS PER MATCH WITH UNMATCHED ROWS
              AFTER MATCH SKIP TO FIRST B
              PATTERN (^ A+? (B | C){2,5}? {- D+ -} $)
              SUBSET U = (B, C)
              DEFINE A AS A.amount > 0,
                     B AS B.amount > PREV(B.amount),
                     C AS C.amount > 0,
                     D AS D.amount > 0
            ) mr
            """);

        assertTrue(parsed.ok(), parsed.errorMessage());
        String sql = renderer.render(parsed.value()).sql();
        var reparsed = parser.parse(Query.class, sql);

        assertTrue(reparsed.ok(), reparsed.errorMessage());
        assertEquals(Utils.canonicalJson(parsed.value()), Utils.canonicalJson(reparsed.value()));
    }

    @Test
    void unsupportedDialectsRejectSharedRendererInsteadOfEmittingSyntax() {
        var parsed = parser.parse(Query.class,
            "SELECT * FROM sales MATCH_RECOGNIZE (PATTERN (A) DEFINE A AS amount > 0)");
        assertTrue(parsed.ok(), parsed.errorMessage());

        assertThrows(UnsupportedDialectFeatureException.class,
            () -> RenderContext.of(new PostgresDialect()).render(parsed.value()));
        assertThrows(UnsupportedDialectFeatureException.class,
            () -> RenderContext.of(new MySqlDialect()).render(parsed.value()));
        assertThrows(UnsupportedDialectFeatureException.class,
            () -> RenderContext.of(new SqlServerDialect()).render(parsed.value()));
    }

    private static final class PatternSpecs extends AnsiSpecs {
        @Override public DialectCapabilities capabilities() {
            return VersionedDialectCapabilities.builder(SqlDialectVersion.of(2016))
                .supports(SqlFeature.MATCH_RECOGNIZE)
                .supports(SqlFeature.CUSTOM_OPERATOR)
                .build();
        }
    }

    private static final class PatternDialect extends AnsiDialect {
        private final DialectCapabilities capabilities = VersionedDialectCapabilities
            .builder(SqlDialectVersion.of(2016))
            .supports(SqlFeature.MATCH_RECOGNIZE)
            .supports(SqlFeature.CUSTOM_OPERATOR)
            .build();

        @Override public DialectCapabilities capabilities() {
            return capabilities;
        }
    }
}
