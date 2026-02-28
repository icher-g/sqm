package io.sqm.middleware.core;

import io.sqm.middleware.api.SqlMiddlewareService;

import java.util.Objects;

/**
 * Runtime container that exposes middleware service and schema bootstrap status.
 *
 * @param service               configured middleware service
 * @param schemaBootstrapStatus schema bootstrap status
 */
public record SqlMiddlewareRuntime(
    SqlMiddlewareService service,
    SchemaBootstrapStatus schemaBootstrapStatus
) {
    /**
     * Creates validated runtime container.
     *
     * @param service               configured middleware service
     * @param schemaBootstrapStatus schema bootstrap status
     */
    public SqlMiddlewareRuntime {
        Objects.requireNonNull(service, "service must not be null");
        Objects.requireNonNull(schemaBootstrapStatus, "schemaBootstrapStatus must not be null");
    }
}

