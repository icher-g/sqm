package io.sqm.render.ansi;

import io.sqm.core.JsonTableBehavior;
import io.sqm.core.JsonTableScalarColumn;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class JsonTableRefRendererTest {
    @Test
    void rendersJsonTableWhenFeatureEnabled() {
        var table = jsonTable(
            col("payload"),
            jsonPath("$.items[*]"),
                jsonScalar(
                    id("id"),
                    type("NUMBER"),
                    jsonPath("$.id"),
                    JsonTableScalarColumn.Wrapper.DEFAULT,
                    jsonBehavior(JsonTableBehavior.Kind.NULL),
                    jsonBehavior(JsonTableBehavior.Kind.ERROR)
                ),
                jsonOrdinality("ord")
        ).as("jt");

        var sql = RenderContext.of(new JsonTableDialect()).render(table);

        assertEquals(
            "JSON_TABLE ( payload, '$.items[*]' COLUMNS ( id NUMBER PATH '$.id' NULL ON EMPTY ERROR ON ERROR, ord FOR ORDINALITY ) ) AS jt",
            sql.sql().replaceAll("\\s+", " ").trim()
        );
    }

    @Test
    void rejectsJsonTableWhenFeatureDisabled() {
        var table = jsonTable(col("payload"), jsonPath("$"), jsonScalar("id", type("NUMBER"), jsonPath("$.id")));

        assertThrows(UnsupportedDialectFeatureException.class, () -> RenderContext.of(new AnsiDialect()).render(table).sql());
    }

    @Test
    void rendersNestedExistsWrappersDefaultBehaviorAndQuotedPaths() {
        var table = jsonTable(
            col("payload"),
            jsonPath("$.items['odd']"),
            jsonScalar(
                id("items"),
                type("JSON"),
                jsonPath("$.items"),
                JsonTableScalarColumn.Wrapper.WITH,
                null,
                jsonBehavior(JsonTableBehavior.Kind.DEFAULT, lit("[]"))
            ),
            jsonScalar(
                id("maybe_items"),
                type("JSON"),
                jsonPath("$.maybe"),
                JsonTableScalarColumn.Wrapper.CONDITIONAL,
                jsonBehavior(JsonTableBehavior.Kind.EMPTY),
                null
            ),
            jsonScalar(
                id("raw_item"),
                type("JSON"),
                jsonPath("$.raw"),
                JsonTableScalarColumn.Wrapper.WITHOUT,
                null,
                null
            ),
            jsonExists(id("present"), type("BOOLEAN"), jsonPath("$.present"), jsonBehavior(JsonTableBehavior.Kind.NULL)),
            jsonNested(jsonPath("$.children[*]"), jsonScalar("child_id", type("NUMBER"), jsonPath("$.id")))
        ).as("jt");

        var sql = RenderContext.of(new JsonTableDialect()).render(table).sql().replaceAll("\\s+", " ").trim();

        assertTrue(sql.contains("'$.items[''odd'']'"));
        assertTrue(sql.contains("items JSON PATH '$.items' WITH WRAPPER DEFAULT '[]' ON ERROR"));
        assertTrue(sql.contains("maybe_items JSON PATH '$.maybe' WITH CONDITIONAL WRAPPER EMPTY ON EMPTY"));
        assertTrue(sql.contains("raw_item JSON PATH '$.raw' WITHOUT WRAPPER"));
        assertTrue(sql.contains("present BOOLEAN EXISTS PATH '$.present' NULL ON ERROR"));
        assertTrue(sql.contains("NESTED PATH '$.children[*]' COLUMNS ( child_id NUMBER PATH '$.id' )"));
    }

    private static final class JsonTableDialect extends AnsiDialect {
        @Override
        public DialectCapabilities capabilities() {
            return feature -> feature == SqlFeature.JSON_TABLE || super.capabilities().supports(feature);
        }
    }
}
