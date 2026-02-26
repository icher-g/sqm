package io.sqm.middleware.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlMiddlewareRuntimeFactoryTest {

    @Test
    void creates_service_with_default_manual_schema_when_no_properties_set() {
        withProperty("sqm.middleware.schema.source", null, () -> {
            var service = SqlMiddlewareRuntimeFactory.createFromEnvironment();
            assertNotNull(service);
        });
    }

    @Test
    void throws_for_unknown_schema_source() {
        withProperty("sqm.middleware.schema.source", "unknown", () ->
            assertThrows(IllegalArgumentException.class, SqlMiddlewareRuntimeFactory::createFromEnvironment)
        );
    }

    @Test
    void supports_disabling_rewrite_pipeline_from_property() {
        withProperty("sqm.middleware.rewrite.enabled", "false", () ->
            assertDoesNotThrow(SqlMiddlewareRuntimeFactory::createFromEnvironment)
        );
    }

    @Test
    void applies_validation_and_guardrail_properties_without_errors() {
        withProperty("sqm.middleware.validation.maxJoinCount", "3", () ->
            withProperty("sqm.middleware.validation.maxSelectColumns", "20", () ->
                withProperty("sqm.middleware.guardrails.maxSqlLength", "5000", () ->
                    withProperty("sqm.middleware.guardrails.timeoutMillis", "2000", () ->
                        withProperty("sqm.middleware.guardrails.maxRows", "100", () ->
                            assertDoesNotThrow(SqlMiddlewareRuntimeFactory::createFromEnvironment)
                        )
                    )
                )
            )
        );
    }

    private void withProperty(String key, String value, Runnable runnable) {
        var previous = System.getProperty(key);
        try {
            if (value == null) {
                System.clearProperty(key);
            }
            else {
                System.setProperty(key, value);
            }
            runnable.run();
        }
        finally {
            if (previous == null) {
                System.clearProperty(key);
            }
            else {
                System.setProperty(key, previous);
            }
        }
    }
}
