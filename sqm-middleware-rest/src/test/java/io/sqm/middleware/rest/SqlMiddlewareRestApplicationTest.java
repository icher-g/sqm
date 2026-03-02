package io.sqm.middleware.rest;

import io.sqm.middleware.api.SqlMiddlewareService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlMiddlewareRestApplicationTest {

    @Test
    void bean_factory_methods_return_expected_types() {
        var app = new SqlMiddlewareRestApplication();
        var runtime = app.sqlMiddlewareRuntime(new MockEnvironment());
        var service = app.sqlMiddlewareService(runtime);
        var adapter = app.sqlMiddlewareRestAdapter(service);

        assertNotNull(runtime);
        assertNotNull(service);
        assertNotNull(adapter);
    }

    @Test
    void rest_adapter_factory_accepts_external_service_instance() {
        var app = new SqlMiddlewareRestApplication();
        SqlMiddlewareService service = app.sqlMiddlewareService(app.sqlMiddlewareRuntime(new MockEnvironment()));

        var adapter = app.sqlMiddlewareRestAdapter(service);

        assertNotNull(adapter);
    }

    @Test
    void spring_property_is_used_as_fallback_when_no_jvm_or_env_override_exists() {
        var key = "sqm.middleware.test.fallback." + System.nanoTime();
        try {
            System.clearProperty(key);
            var env = new MockEnvironment().withProperty(key, "from-spring");

            SqlMiddlewareRestApplication.applySpringFallbackProperty(
                env,
                io.sqm.control.ConfigKeys.Key.of(key, "SQM_TEST_ENV_" + System.nanoTime())
            );

            assertEquals("from-spring", System.getProperty(key));
        } finally {
            System.clearProperty(key);
        }
    }

    @Test
    void spring_fallback_does_not_override_existing_jvm_property() {
        var key = "sqm.middleware.test.precedence." + System.nanoTime();
        try {
            System.setProperty(key, "from-jvm");
            var env = new MockEnvironment().withProperty(key, "from-spring");

            SqlMiddlewareRestApplication.applySpringFallbackProperty(
                env,
                io.sqm.control.ConfigKeys.Key.of(key, "SQM_TEST_ENV_" + System.nanoTime())
            );

            assertEquals("from-jvm", System.getProperty(key));
        } finally {
            System.clearProperty(key);
        }
    }
}
