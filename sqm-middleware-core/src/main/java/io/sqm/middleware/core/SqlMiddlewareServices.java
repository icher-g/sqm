package io.sqm.middleware.core;

import io.sqm.control.SqlDecisionService;
import io.sqm.control.SqlDecisionServiceConfig;
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
    public static SqlMiddlewareService create(SqlDecisionServiceConfig config) {
        Objects.requireNonNull(config, "config must not be null");
        return new SqlMiddlewareCoreService(SqlDecisionService.create(config));
    }

    /**
     * Creates a transport-neutral middleware service from an existing middleware instance.
     *
     * @param decisionService decision service instance
     * @return service instance
     */
    public static SqlMiddlewareService create(SqlDecisionService decisionService) {
        Objects.requireNonNull(decisionService, "decisionService must not be null");
        return new SqlMiddlewareCoreService(decisionService);
    }
}

