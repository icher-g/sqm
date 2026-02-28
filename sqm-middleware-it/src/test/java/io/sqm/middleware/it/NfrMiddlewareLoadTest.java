package io.sqm.middleware.it;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.control.SqlDecisionServiceConfig;
import io.sqm.middleware.api.AnalyzeRequest;
import io.sqm.middleware.api.ExecutionContextDto;
import io.sqm.middleware.core.SqlMiddlewareServices;
import io.sqm.middleware.mcp.SqlMiddlewareMcpAdapter;
import io.sqm.middleware.mcp.SqlMiddlewareMcpToolRouter;
import io.sqm.middleware.rest.SqlMiddlewareRestAdapter;
import io.sqm.middleware.rest.SqlMiddlewareRestController;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class NfrMiddlewareLoadTest {

    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "users",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("name", CatalogType.STRING)
        )
    );

    @Test
    void rest_and_mcp_concurrent_load_meet_nfr_thresholds() throws Exception {
        int totalRequests = intProp("sqm.nfr.concurrent.requests", 300);
        int threads = intProp("sqm.nfr.concurrent.threads", 12);
        long maxP95Millis = intProp("sqm.nfr.max.p95.ms", 250);
        long minThroughputRps = intProp("sqm.nfr.min.throughput.rps", 200);

        var service = SqlMiddlewareServices.create(
            SqlDecisionServiceConfig.builder(SCHEMA).buildValidationAndRewriteConfig()
        );
        var restController = new SqlMiddlewareRestController(new SqlMiddlewareRestAdapter(service));
        var mcpRouter = new SqlMiddlewareMcpToolRouter(new SqlMiddlewareMcpAdapter(service));

        var restResult = runConcurrent(totalRequests, threads, () -> {
            var request = new AnalyzeRequest("select id from users", new ExecutionContextDto("postgresql", null, null, null, null));
            var decision = restController.analyze(request);
            assertNotNull(decision);
        });
        assertTrue(restResult.p95Millis <= maxP95Millis, "REST p95 too high: " + restResult.p95Millis + "ms");
        assertTrue(restResult.throughputRps >= minThroughputRps, "REST throughput too low: " + restResult.throughputRps + " rps");

        var mcpResult = runConcurrent(totalRequests, threads, () -> {
            var request = new AnalyzeRequest("select id from users", new ExecutionContextDto("postgresql", null, null, null, null));
            var decision = (io.sqm.middleware.api.DecisionResultDto) mcpRouter.invoke(SqlMiddlewareMcpToolRouter.ANALYZE_TOOL, request);
            assertNotNull(decision);
        });
        assertTrue(mcpResult.p95Millis <= maxP95Millis, "MCP p95 too high: " + mcpResult.p95Millis + "ms");
        assertTrue(mcpResult.throughputRps >= minThroughputRps, "MCP throughput too low: " + mcpResult.throughputRps + " rps");
    }

    private static RunResult runConcurrent(int totalRequests, int threads, ThrowingRunnable action) throws Exception {
        try (var pool = Executors.newFixedThreadPool(threads)) {
            try {
                List<Long> latencies = Collections.synchronizedList(new ArrayList<>(totalRequests));
                var failures = new AtomicInteger();
                long started = System.nanoTime();

                List<Callable<Void>> tasks = new ArrayList<>(totalRequests);
                for (int i = 0; i < totalRequests; i++) {
                    tasks.add(() -> {
                        long t0 = System.nanoTime();
                        try {
                            action.run();
                        } catch (Throwable ex) {
                            failures.incrementAndGet();
                            throw ex;
                        } finally {
                            latencies.add(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0));
                        }
                        return null;
                    });
                }

                List<Future<Void>> futures = pool.invokeAll(tasks);
                for (Future<Void> future : futures) {
                    future.get();
                }

                long elapsedNanos = System.nanoTime() - started;
                long elapsedMillis = Math.max(1, TimeUnit.NANOSECONDS.toMillis(elapsedNanos));
                long throughput = (totalRequests * 1000L) / elapsedMillis;
                long p95 = percentile(latencies, 95);

                assertEquals(0, failures.get(), "Unexpected failed operations under load");
                return new RunResult(p95, throughput);
            } finally {
                pool.shutdownNow();
            }
        }
    }

    private static long percentile(List<Long> values, int percentile) {
        if (values.isEmpty()) {
            return 0L;
        }
        var sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int index = (int) Math.ceil((percentile / 100.0) * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index);
    }

    private static int intProp(String key, int defaultValue) {
        var raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(raw.trim());
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private record RunResult(long p95Millis, long throughputRps) {
    }
}
