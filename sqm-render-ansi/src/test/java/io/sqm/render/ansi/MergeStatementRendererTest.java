package io.sqm.render.ansi;

import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.ansi.spi.AnsiDialect;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.merge;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MergeStatementRendererTest {

    @Test
    void rejectsMergeInAnsiRenderer() {
        var mergeStatement = merge("users")
            .source(tbl("src"))
            .on(col("users", "id").eq(col("src", "id")))
            .whenMatchedDelete()
            .build();

        assertThrows(UnsupportedDialectFeatureException.class, () -> RenderContext.of(new AnsiDialect()).render(mergeStatement));
    }
}
