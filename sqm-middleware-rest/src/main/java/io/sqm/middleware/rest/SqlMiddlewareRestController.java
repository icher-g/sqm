package io.sqm.middleware.rest;

import io.sqm.middleware.api.AnalyzeRequest;
import io.sqm.middleware.api.DecisionExplanationDto;
import io.sqm.middleware.api.DecisionResultDto;
import io.sqm.middleware.api.EnforceRequest;
import io.sqm.middleware.api.ExplainRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * HTTP controller exposing SQL middleware operations.
 */
@RestController
@RequestMapping("/sqm/middleware")
public final class SqlMiddlewareRestController {

    private final SqlMiddlewareRestAdapter adapter;

    /**
     * Creates a controller backed by a REST adapter.
     *
     * @param adapter REST adapter
     */
    public SqlMiddlewareRestController(SqlMiddlewareRestAdapter adapter) {
        this.adapter = Objects.requireNonNull(adapter, "adapter must not be null");
    }

    /**
     * Handles HTTP analyze requests.
     *
     * @param request analyze request payload
     * @return decision result
     */
    @PostMapping("/analyze")
    public DecisionResultDto analyze(@RequestBody AnalyzeRequest request) {
        return adapter.analyze(request);
    }

    /**
     * Handles HTTP enforce requests.
     *
     * @param request enforce request payload
     * @return decision result
     */
    @PostMapping("/enforce")
    public DecisionResultDto enforce(@RequestBody EnforceRequest request) {
        return adapter.enforce(request);
    }

    /**
     * Handles HTTP explain requests.
     *
     * @param request explain request payload
     * @return decision explanation
     */
    @PostMapping("/explain")
    public DecisionExplanationDto explain(@RequestBody ExplainRequest request) {
        return adapter.explain(request);
    }
}
