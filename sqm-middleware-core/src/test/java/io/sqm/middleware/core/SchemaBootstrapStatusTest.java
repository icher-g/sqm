package io.sqm.middleware.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaBootstrapStatusTest {

    @Test
    void ready_factory_creates_ready_status_without_error() {
        var status = SchemaBootstrapStatus.ready("manual", "manual bundled resource");
        assertTrue(status.ready());
        assertEquals(SchemaBootstrapStatus.State.READY, status.state());
        assertEquals("manual", status.source());
        assertEquals("manual bundled resource", status.description());
        assertNull(status.error());
    }

    @Test
    void degraded_factory_creates_degraded_status_with_error() {
        var status = SchemaBootstrapStatus.degraded("json", "json file ./schema.json", "parse failure");
        assertFalse(status.ready());
        assertEquals(SchemaBootstrapStatus.State.DEGRADED, status.state());
        assertEquals("json", status.source());
        assertNotNull(status.error());
    }

    @Test
    void factories_reject_blank_arguments() {
        assertThrows(IllegalArgumentException.class, () -> SchemaBootstrapStatus.ready(" ", "x"));
        assertThrows(IllegalArgumentException.class, () -> SchemaBootstrapStatus.ready("json", " "));
        assertThrows(IllegalArgumentException.class, () -> SchemaBootstrapStatus.degraded("json", "x", " "));
    }
}
