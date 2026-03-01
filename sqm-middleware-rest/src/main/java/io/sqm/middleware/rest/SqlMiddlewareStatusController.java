package io.sqm.middleware.rest;

import io.sqm.middleware.core.SqlMiddlewareRuntime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * HTTP status controller exposing middleware bootstrap health and readiness.
 */
@RestController
@RequestMapping("/sqm/middleware/v1")
public final class SqlMiddlewareStatusController {

    private final SqlMiddlewareRuntime runtime;

    /**
     * Creates status controller.
     *
     * @param runtime middleware runtime diagnostics
     */
    public SqlMiddlewareStatusController(SqlMiddlewareRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime must not be null");
    }

    /**
     * Returns liveness-style host health.
     *
     * @return health status response
     */
    @GetMapping("/health")
    public SqlMiddlewareStatusResponse health() {
        var schema = runtime.schemaBootstrapStatus();
        return new SqlMiddlewareStatusResponse(
            "UP",
            schema.source(),
            schema.state().name(),
            schema.description(),
            schema.error()
        );
    }

    /**
     * Returns readiness status reflecting schema bootstrap availability.
     *
     * @return readiness status response with HTTP 200 when ready and HTTP 503 when not ready
     */
    @GetMapping("/readiness")
    public ResponseEntity<SqlMiddlewareStatusResponse> readiness() {
        var schema = runtime.schemaBootstrapStatus();
        var response = new SqlMiddlewareStatusResponse(
            schema.ready() ? "READY" : "NOT_READY",
            schema.source(),
            schema.state().name(),
            schema.description(),
            schema.error()
        );
        return ResponseEntity.status(schema.ready() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}

