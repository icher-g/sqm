package io.sqm.middleware.mcp;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SqlMiddlewareMcpApplicationMainTest {

    @Test
    void private_constructor_exists_and_is_invocable_for_coverage() throws Exception {
        Constructor<SqlMiddlewareMcpApplication> constructor = SqlMiddlewareMcpApplication.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        var instance = constructor.newInstance();
        assertNotNull(instance);
    }

    @Test
    void main_wraps_io_failure_from_server_loop() {
        var originalIn = System.in;
        var originalOut = System.out;
        var originalSchemaSource = System.getProperty("sqm.middleware.schema.source");
        var originalRewriteEnabled = System.getProperty("sqm.middleware.rewrite.enabled");
        try {
            System.setProperty("sqm.middleware.schema.source", "manual");
            System.setProperty("sqm.middleware.rewrite.enabled", "false");
            System.setIn(new ByteArrayInputStream("Content-Type: application/json\r\n\r\n{}".getBytes(StandardCharsets.UTF_8)));
            System.setOut(new java.io.PrintStream(new ByteArrayOutputStream()));

            var error = assertThrows(IllegalStateException.class, () -> SqlMiddlewareMcpApplication.main(new String[0]));
            assertTrue(error.getMessage().contains("Failed to run MCP stdio server"));
            assertNotNull(error.getCause());
            assertTrue(error.getCause().getMessage().contains("Missing Content-Length"));
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
            restoreProperty("sqm.middleware.schema.source", originalSchemaSource);
            restoreProperty("sqm.middleware.rewrite.enabled", originalRewriteEnabled);
        }
    }

    private void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        }
        else {
            System.setProperty(key, value);
        }
    }
}
