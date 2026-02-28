package io.sqm.middleware.core;

import io.sqm.middleware.api.*;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * {@link SqlMiddlewareService} decorator that enforces in-flight concurrency limits and host-level request timeout.
 */
public final class FlowControlSqlMiddlewareService implements SqlMiddlewareService {

    private final SqlMiddlewareService delegate;
    private final Semaphore inFlight;
    private final Long acquireTimeoutMillis;
    private final Long requestTimeoutMillis;

    /**
     * Creates a flow-control wrapper.
     *
     * @param delegate             wrapped middleware service
     * @param maxInFlight          maximum concurrent in-flight requests; when {@code null}, concurrency limit is disabled
     * @param acquireTimeoutMillis optional semaphore acquire timeout in milliseconds; when {@code null}, acquire is non-blocking
     * @param requestTimeoutMillis optional host-level request timeout in milliseconds
     */
    public FlowControlSqlMiddlewareService(
        SqlMiddlewareService delegate,
        Integer maxInFlight,
        Long acquireTimeoutMillis,
        Long requestTimeoutMillis
    ) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        if (maxInFlight != null && maxInFlight <= 0) {
            throw new IllegalArgumentException("maxInFlight must be > 0");
        }
        if (acquireTimeoutMillis != null && acquireTimeoutMillis < 0) {
            throw new IllegalArgumentException("acquireTimeoutMillis must be >= 0");
        }
        if (requestTimeoutMillis != null && requestTimeoutMillis <= 0) {
            throw new IllegalArgumentException("requestTimeoutMillis must be > 0");
        }
        this.inFlight = maxInFlight == null ? null : new Semaphore(maxInFlight);
        this.acquireTimeoutMillis = acquireTimeoutMillis;
        this.requestTimeoutMillis = requestTimeoutMillis;
    }

    private static DecisionResultDto deny(ReasonCodeDto reasonCode, String message) {
        return new DecisionResultDto(
            DecisionKindDto.DENY,
            reasonCode,
            message,
            null,
            List.of(),
            null,
            null
        );
    }

    /**
     * Delegates analyze with flow control.
     *
     * @param request analyze request payload
     * @return decision result payload
     */
    @Override
    public DecisionResultDto analyze(AnalyzeRequest request) {
        return withFlowControl("analyze", () -> runWithTimeout(() -> delegate.analyze(request)));
    }

    /**
     * Delegates enforce with flow control.
     *
     * @param request enforce request payload
     * @return decision result payload
     */
    @Override
    public DecisionResultDto enforce(EnforceRequest request) {
        return withFlowControl("enforce", () -> runWithTimeout(() -> delegate.enforce(request)));
    }

    /**
     * Delegates explain with flow control.
     *
     * @param request explain request payload
     * @return decision explanation payload
     */
    @Override
    public DecisionExplanationDto explainDecision(ExplainRequest request) {
        return withFlowControl("explain", () -> runWithTimeout(() -> delegate.explainDecision(request)));
    }

    private <T> T withFlowControl(String operation, Call<T> call) {
        var acquired = acquirePermit();
        if (!acquired) {
            return flowControlDeny(operation);
        }

        try {
            return call.call();
        } catch (TimeoutException ex) {
            return timeoutDeny(operation);
        } catch (ExecutionException ex) {
            return pipelineDeny(operation, ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return timeoutDeny(operation);
        } catch (Exception ex) {
            return pipelineDeny(operation, ex.getMessage());
        } finally {
            if (inFlight != null) {
                inFlight.release();
            }
        }
    }

    private boolean acquirePermit() {
        if (inFlight == null) {
            return true;
        }

        try {
            if (acquireTimeoutMillis == null) {
                return inFlight.tryAcquire();
            }
            return inFlight.tryAcquire(acquireTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private <T> T runWithTimeout(Call<T> call) throws Exception {
        if (requestTimeoutMillis == null) {
            return call.call();
        }

        var future = CompletableFuture.supplyAsync(() -> {
            try {
                return call.call();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        try {
            return future.get(requestTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | TimeoutException | ExecutionException ex) {
            future.cancel(true);
            throw ex;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T flowControlDeny(String operation) {
        var decision = deny(ReasonCodeDto.DENY_PIPELINE_ERROR, "Request rejected by backpressure (%s): max in-flight reached".formatted(operation));
        if ("explain".equals(operation)) {
            return (T) new DecisionExplanationDto(decision, decision.message());
        }
        return (T) decision;
    }

    @SuppressWarnings("unchecked")
    private <T> T timeoutDeny(String operation) {
        var decision = deny(ReasonCodeDto.DENY_TIMEOUT, "Request timed out in host flow-control (%s)".formatted(operation));
        if ("explain".equals(operation)) {
            return (T) new DecisionExplanationDto(decision, decision.message());
        }
        return (T) decision;
    }

    @SuppressWarnings("unchecked")
    private <T> T pipelineDeny(String operation, String message) {
        var detail = message == null ? "" : message;
        var decision = deny(ReasonCodeDto.DENY_PIPELINE_ERROR, "Pipeline failure in host flow-control (%s): %s".formatted(operation, detail));
        if ("explain".equals(operation)) {
            return (T) new DecisionExplanationDto(decision, decision.message());
        }
        return (T) decision;
    }

    @FunctionalInterface
    private interface Call<T> {
        T call() throws Exception;
    }
}
