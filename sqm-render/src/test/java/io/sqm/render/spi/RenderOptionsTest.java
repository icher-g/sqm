package io.sqm.render.spi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RenderOptionsTest {

    @Test
    void createsOptionsWithMode() {
        var options = RenderOptions.of(ParameterizationMode.Inline);

        assertEquals(ParameterizationMode.Inline, options.parameterizationMode());
    }
}
