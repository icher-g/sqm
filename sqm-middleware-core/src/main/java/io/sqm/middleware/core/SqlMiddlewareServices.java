package io.sqm.middleware.core;

import io.sqm.control.SqlMiddleware;
import io.sqm.control.SqlMiddlewareConfig;
import io.sqm.middleware.api.SqlMiddlewareService;

import java.util.Objects;

/**
 * Factory helpers for creating {@link SqlMiddlewareService} instances.
 */
public final class SqlMiddlewareServices {

    private SqlMiddlewareServices() {
    }

    /**
     * Creates a transport-neutral middleware service from framework configuration.
     *
     * @param config middleware configuration
     * @return service instance
     */
    public static SqlMiddlewareService create(SqlMiddlewareConfig config) {
        Objects.requireNonNull(config, "config must not be null");
        return new SqlMiddlewareCoreService(SqlMiddleware.create(config));
    }

    /**
     * Creates a transport-neutral middleware service from an existing middleware instance.
     *
     * @param middleware middleware instance
     * @return service instance
     */
    public static SqlMiddlewareService create(SqlMiddleware middleware) {
        Objects.requireNonNull(middleware, "middleware must not be null");
        return new SqlMiddlewareCoreService(middleware);
    }
}
