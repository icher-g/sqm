package io.sqm.playground.rest.controller;

import io.sqm.playground.api.ExamplesResponseDto;
import io.sqm.playground.api.ParseRequestDto;
import io.sqm.playground.api.ParseResponseDto;
import io.sqm.playground.api.RenderRequestDto;
import io.sqm.playground.api.RenderResponseDto;
import io.sqm.playground.api.TranspileRequestDto;
import io.sqm.playground.api.TranspileResponseDto;
import io.sqm.playground.api.ValidateRequestDto;
import io.sqm.playground.api.ValidateResponseDto;
import io.sqm.playground.rest.service.ExampleService;
import io.sqm.playground.rest.service.ParseService;
import io.sqm.playground.rest.service.RenderService;
import io.sqm.playground.rest.service.TranspileService;
import io.sqm.playground.rest.service.ValidateService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * HTTP controller exposing playground operations.
 */
@RestController
@RequestMapping("/api/v1")
public final class PlaygroundController {

    private final ExampleService exampleService;
    private final ParseService parseService;
    private final RenderService renderService;
    private final ValidateService validateService;
    private final TranspileService transpileService;

    /**
     * Creates the playground controller.
     *
     * @param exampleService example service
     * @param parseService parse service
     * @param renderService render service
     * @param validateService validate service
     * @param transpileService transpile service
     */
    public PlaygroundController(
        ExampleService exampleService,
        ParseService parseService,
        RenderService renderService,
        ValidateService validateService,
        TranspileService transpileService
    ) {
        this.exampleService = Objects.requireNonNull(exampleService, "exampleService must not be null");
        this.parseService = Objects.requireNonNull(parseService, "parseService must not be null");
        this.renderService = Objects.requireNonNull(renderService, "renderService must not be null");
        this.validateService = Objects.requireNonNull(validateService, "validateService must not be null");
        this.transpileService = Objects.requireNonNull(transpileService, "transpileService must not be null");
    }

    /**
     * Returns the built-in playground examples.
     *
     * @return examples response
     */
    @GetMapping("/examples")
    public ExamplesResponseDto examples() {
        return exampleService.examples();
    }

    /**
     * Parses SQL into the SQM model and serialized JSON.
     *
     * @param request parse request payload
     * @return parse response
     */
    @PostMapping("/parse")
    public ParseResponseDto parse(@RequestBody ParseRequestDto request) {
        return parseService.parse(request);
    }

    /**
     * Parses and renders SQL into the requested target dialect.
     *
     * @param request render request payload
     * @return render response
     */
    @PostMapping("/render")
    public RenderResponseDto render(@RequestBody RenderRequestDto request) {
        return renderService.render(request);
    }

    /**
     * Parses and validates SQL against the selected dialect.
     *
     * @param request validate request payload
     * @return validate response
     */
    @PostMapping("/validate")
    public ValidateResponseDto validate(@RequestBody ValidateRequestDto request) {
        return validateService.validate(request);
    }

    /**
     * Transpiles SQL into the requested target dialect.
     *
     * @param request transpile request payload
     * @return transpile response
     */
    @PostMapping("/transpile")
    public TranspileResponseDto transpile(@RequestBody TranspileRequestDto request) {
        return transpileService.transpile(request);
    }
}
