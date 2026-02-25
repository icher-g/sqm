package io.sqm.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParameterizationModeTest {

    @Test
    void exposes_stable_initial_modes() {
        assertEquals(
            java.util.List.of(ParameterizationMode.OFF, ParameterizationMode.BIND),
            java.util.List.of(ParameterizationMode.values())
        );
    }
}
