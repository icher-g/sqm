package io.sqm.middleware.core;

import io.sqm.middleware.api.AnalyzeRequest;
import io.sqm.middleware.api.DecisionExplanationDto;
import io.sqm.middleware.api.DecisionResultDto;
import io.sqm.middleware.api.EnforceRequest;
import io.sqm.middleware.api.ExplainRequest;
import io.sqm.middleware.api.SqlMiddlewareService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlMiddlewareRuntimeTest {

    @Test
    void constructor_rejects_null_arguments() {
        var ready = SchemaBootstrapStatus.ready("manual", "manual bundled resource");
        assertThrows(NullPointerException.class, () -> new SqlMiddlewareRuntime(null, ready));
        assertThrows(NullPointerException.class, () -> new SqlMiddlewareRuntime(noopService(), null));
    }

    @Test
    void constructor_stores_service_and_status() {
        var service = noopService();
        var status = SchemaBootstrapStatus.ready("manual", "manual bundled resource");
        var runtime = new SqlMiddlewareRuntime(service, status);
        assertSame(service, runtime.service());
        assertSame(status, runtime.schemaBootstrapStatus());
    }

    private static SqlMiddlewareService noopService() {
        return new SqlMiddlewareService() {
            @Override
            public DecisionResultDto analyze(AnalyzeRequest request) {
                return null;
            }

            @Override
            public DecisionResultDto enforce(EnforceRequest request) {
                return null;
            }

            @Override
            public DecisionExplanationDto explainDecision(ExplainRequest request) {
                return null;
            }
        };
    }
}
