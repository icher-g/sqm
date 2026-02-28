package io.sqm.middleware.rest;

import io.sqm.middleware.api.SqlMiddlewareService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SqlMiddlewareRestApplicationTest {

    @Test
    void bean_factory_methods_return_expected_types() {
        var app = new SqlMiddlewareRestApplication();
        var runtime = app.sqlMiddlewareRuntime();
        var service = app.sqlMiddlewareService(runtime);
        var adapter = app.sqlMiddlewareRestAdapter(service);

        assertNotNull(runtime);
        assertNotNull(service);
        assertNotNull(adapter);
    }

    @Test
    void rest_adapter_factory_accepts_external_service_instance() {
        var app = new SqlMiddlewareRestApplication();
        SqlMiddlewareService service = app.sqlMiddlewareService(app.sqlMiddlewareRuntime());

        var adapter = app.sqlMiddlewareRestAdapter(service);

        assertNotNull(adapter);
    }
}
