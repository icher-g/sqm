package io.sqm.middleware.core;

import io.sqm.middleware.api.DecisionResultDto;

/**
 * Telemetry hook for transport-neutral middleware decisions.
 */
@FunctionalInterface
public interface MiddlewareTelemetry {

    /**
     * Records one completed middleware operation.
     *
     * @param operation operation name (analyze/enforce/explain)
     * @param decision decision payload
     * @param durationNanos end-to-end operation latency in nanoseconds
     */
    void record(String operation, DecisionResultDto decision, long durationNanos);

    /**
     * Returns a no-op telemetry hook.
     *
     * @return no-op telemetry
     */
    static MiddlewareTelemetry noop() {
        return (operation, decision, durationNanos) -> {
        };
    }
}
