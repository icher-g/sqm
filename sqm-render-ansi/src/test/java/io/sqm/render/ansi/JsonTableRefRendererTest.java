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

    private static final class JsonTableDialect extends AnsiDialect {
        @Override
        public DialectCapabilities capabilities() {
            return feature -> feature == SqlFeature.JSON_TABLE || super.capabilities().supports(feature);
        }
    }
}
