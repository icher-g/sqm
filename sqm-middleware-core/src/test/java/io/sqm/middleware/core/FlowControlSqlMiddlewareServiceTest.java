package io.sqm.middleware.core;

import io.sqm.middleware.api.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class FlowControlSqlMiddlewareServiceTest {

    @Test
    void rejects_invalid_constructor_arguments() {
        var delegate = new StubService();
        assertThrows(NullPointerException.class, () -> new FlowControlSqlMiddlewareService(null, 1, null, null));
        assertThrows(IllegalArgumentException.class, () -> new FlowControlSqlMiddlewareService(delegate, 0, null, null));
        assertThrows(IllegalArgumentException.class, () -> new FlowControlSqlMiddlewareService(delegate, 1, -1L, null));
        assertThrows(IllegalArgumentException.class, () -> new FlowControlSqlMiddlewareService(delegate, 1, null, 0L));
    }

    @Test
    void returns_backpressure_deny_when_saturated() throws Exception {
        var blocking = new BlockingService();
        var service = new FlowControlSqlMiddlewareService(blocking, 1, null, null);
        var context = new ExecutionContextDto("postgresql", null, null, null, null);

        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        blocking.setLatches(started, release);
        var t = Thread.startVirtualThread(() -> service.analyze(new AnalyzeRequest("select 1", context)));
        assertTrue(started.await(1, TimeUnit.SECONDS));

        var denied = service.analyze(new AnalyzeRequest("select 1", context));
        assertEquals(DecisionKindDto.DENY, denied.kind());
        assertEquals(ReasonCodeDto.DENY_PIPELINE_ERROR, denied.reasonCode());

        release.countDown();
        t.join();
    }

    @Test
    void returns_timeout_deny_when_request_exceeds_host_timeout() {
        var service = new FlowControlSqlMiddlewareService(new SlowService(200), null, null, 20L);
        var result = service.analyze(new AnalyzeRequest("select 1", new ExecutionContextDto("postgresql", null, null, null, null)));

        assertEquals(DecisionKindDto.DENY, result.kind());
        assertEquals(ReasonCodeDto.DENY_TIMEOUT, result.reasonCode());
    }

    @Test
    void returns_timeout_decision_in_explain_shape_when_request_exceeds_host_timeout() {
        var service = new FlowControlSqlMiddlewareService(new SlowService(200), null, null, 20L);
        var explanation = service.explainDecision(new ExplainRequest("select 1", new ExecutionContextDto("postgresql", null, null, null, null)));

        assertEquals(DecisionKindDto.DENY, explanation.decision().kind());
        assertEquals(ReasonCodeDto.DENY_TIMEOUT, explanation.decision().reasonCode());
    }

    @Test
    void returns_pipeline_error_when_delegate_throws_in_enforce() {
        var service = new FlowControlSqlMiddlewareService(new ThrowingService(), null, null, null);
        var result = service.enforce(new EnforceRequest("select 1", new ExecutionContextDto("postgresql", null, null, null, null)));
        assertEquals(DecisionKindDto.DENY, result.kind());
        assertEquals(ReasonCodeDto.DENY_PIPELINE_ERROR, result.reasonCode());
        assertTrue(result.message().contains("Pipeline failure"));
    }

    @Test
    void returns_pipeline_error_explain_shape_when_delegate_throws_in_explain() {
        var service = new FlowControlSqlMiddlewareService(new ThrowingService(), null, null, null);
        var explanation = service.explainDecision(new ExplainRequest("select 1", new ExecutionContextDto("postgresql", null, null, null, null)));
        assertEquals(DecisionKindDto.DENY, explanation.decision().kind());
        assertEquals(ReasonCodeDto.DENY_PIPELINE_ERROR, explanation.decision().reasonCode());
    }

    @Test
    void returns_pipeline_error_when_delegate_throws_inside_timed_execution() {
        var service = new FlowControlSqlMiddlewareService(new ThrowingService(), null, null, 1000L);
        var result = service.enforce(new EnforceRequest("select 1", new ExecutionContextDto("postgresql", null, null, null, null)));
        assertEquals(DecisionKindDto.DENY, result.kind());
        assertEquals(ReasonCodeDto.DENY_PIPELINE_ERROR, result.reasonCode());
    }

    @Test
    void supports_blocking_acquire_with_timeout_then_recovers_after_release() throws Exception {
        var blocking = new BlockingService();
        var service = new FlowControlSqlMiddlewareService(blocking, 1, 5L, null);
        var context = new ExecutionContextDto("postgresql", null, null, null, null);

        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        blocking.setLatches(started, release);
        var t = Thread.startVirtualThread(() -> service.analyze(new AnalyzeRequest("select 1", context)));
        assertTrue(started.await(1, TimeUnit.SECONDS));

        var denied = service.analyze(new AnalyzeRequest("select 1", context));
        assertEquals(DecisionKindDto.DENY, denied.kind());
        assertEquals(ReasonCodeDto.DENY_PIPELINE_ERROR, denied.reasonCode());

        release.countDown();
        t.join();

        var allowed = service.analyze(new AnalyzeRequest("select 1", context));
        assertEquals(DecisionKindDto.ALLOW, allowed.kind());
    }

    private static class StubService implements SqlMiddlewareService {
        @Override
        public DecisionResultDto analyze(AnalyzeRequest request) {
            return new DecisionResultDto(DecisionKindDto.ALLOW, ReasonCodeDto.NONE, null, null, List.of(), null, null);
        }

        @Override
        public DecisionResultDto enforce(EnforceRequest request) {
            return new DecisionResultDto(DecisionKindDto.ALLOW, ReasonCodeDto.NONE, null, null, List.of(), null, null);
        }

        @Override
        public DecisionExplanationDto explainDecision(ExplainRequest request) {
            return new DecisionExplanationDto(
                new DecisionResultDto(DecisionKindDto.ALLOW, ReasonCodeDto.NONE, null, null, List.of(), null, null),
                "ok"
            );
        }
    }

    private static final class SlowService extends StubService {
        private final long delayMillis;

        private SlowService(long delayMillis) {
            this.delayMillis = delayMillis;
        }

        @Override
        public DecisionResultDto analyze(AnalyzeRequest request) {
            sleep();
            return super.analyze(request);
        }

        @Override
        public DecisionExplanationDto explainDecision(ExplainRequest request) {
            sleep();
            return super.explainDecision(request);
        }

        private void sleep() {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static final class BlockingService extends StubService {
        private CountDownLatch started;
        private CountDownLatch release;

        private void setLatches(CountDownLatch started, CountDownLatch release) {
            this.started = started;
            this.release = release;
        }

        @Override
        public DecisionResultDto analyze(AnalyzeRequest request) {
            started.countDown();
            try {
                var ignored = release.await(1, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return super.analyze(request);
        }
    }

    private static final class ThrowingService extends StubService {
        @Override
        public DecisionResultDto enforce(EnforceRequest request) {
            throw new IllegalStateException("boom");
        }

        @Override
        public DecisionExplanationDto explainDecision(ExplainRequest request) {
            throw new IllegalStateException("boom");
        }
    }
}
