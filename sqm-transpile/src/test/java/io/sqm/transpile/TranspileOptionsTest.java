package io.sqm.transpile;

import io.sqm.render.spi.ParameterizationMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranspileOptionsTest {

    @Test
    void defaultsUseInlineRendering() {
        var options = TranspileOptions.defaults();

        assertEquals(ParameterizationMode.Inline, options.renderParameterizationMode());
    }

    @Test
    void legacyConstructorUsesInlineRendering() {
        var options = new TranspileOptions(true, false, true, true);

        assertEquals(ParameterizationMode.Inline, options.renderParameterizationMode());
    }

    @Test
    void nullRenderParameterizationModeDefaultsToInline() {
        var options = new TranspileOptions(true, false, true, true, null);

        assertEquals(ParameterizationMode.Inline, options.renderParameterizationMode());
    }
}
