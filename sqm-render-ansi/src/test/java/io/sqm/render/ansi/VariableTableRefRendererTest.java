package io.sqm.render.ansi;

import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.ansi.spi.AnsiDialect;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.tableVar;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VariableTableRefRendererTest {

    @Test
    void rejectsVariableTableRenderingInAnsiDialect() {
        var ctx = RenderContext.of(new AnsiDialect());

        assertThrows(UnsupportedDialectFeatureException.class, () -> ctx.render(tableVar("audit")));
    }
}
