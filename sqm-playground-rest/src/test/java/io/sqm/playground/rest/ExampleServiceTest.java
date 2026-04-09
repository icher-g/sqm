package io.sqm.playground.rest;

import io.sqm.playground.rest.example.ExampleCatalog;
import io.sqm.playground.rest.service.ExampleService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests built-in playground examples service behavior.
 */
class ExampleServiceTest {

    @Test
    void examplesResponseContainsBuiltInExamples() {
        var service = new ExampleService(new ExampleCatalog());

        var response = service.examples();

        assertTrue(response.success());
        assertNotNull(response.requestId());
        assertFalse(response.requestId().isBlank());
        assertEquals(4, response.examples().size());
        assertEquals("basic-select", response.examples().getFirst().id());
        assertEquals("ansi", response.examples().getFirst().dialect().name());
    }
}
