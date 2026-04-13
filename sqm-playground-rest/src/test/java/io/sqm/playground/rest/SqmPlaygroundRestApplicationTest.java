package io.sqm.playground.rest;

import io.sqm.playground.rest.controller.HealthController;
import io.sqm.playground.rest.controller.PlaygroundController;
import io.sqm.playground.rest.example.ExampleCatalog;
import io.sqm.playground.rest.service.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests basic playground REST application wiring.
 */
class SqmPlaygroundRestApplicationTest {

    @Test
    void applicationAndHealthControllerCanBeConstructed() {
        var app = new SqmPlaygroundRestApplication();
        var controller = new HealthController();
        var statementSupport = new PlaygroundStatementSupport();
        var parseService = new ParseService(new SqmAstMapper(), new SqmDslGenerator(new InMemoryCompiler()), statementSupport);
        var renderService = new RenderService(statementSupport);
        var validateService = new ValidateService(statementSupport);
        var transpileService = new TranspileService(statementSupport);
        var playgroundController = new PlaygroundController(
            new ExampleService(new ExampleCatalog()),
            parseService,
            renderService,
            validateService,
            transpileService
        );

        assertNotNull(app);
        assertNotNull(controller);
        assertNotNull(controller.health());
        assertNotNull(parseService);
        assertNotNull(renderService);
        assertNotNull(validateService);
        assertNotNull(transpileService);
        assertNotNull(playgroundController);
    }
}
