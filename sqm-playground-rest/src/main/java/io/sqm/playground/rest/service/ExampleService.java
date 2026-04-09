package io.sqm.playground.rest.service;

import io.sqm.playground.api.ExamplesResponseDto;
import io.sqm.playground.rest.example.ExampleCatalog;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Service providing built-in playground examples.
 */
@Service
public final class ExampleService {

    private final ExampleCatalog catalog;

    /**
     * Creates the example service.
     *
     * @param catalog example catalog
     */
    public ExampleService(ExampleCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
    }

    /**
     * Returns built-in examples wrapped in the shared response contract.
     *
     * @return examples response
     */
    public ExamplesResponseDto examples() {
        return new ExamplesResponseDto(
            UUID.randomUUID().toString(),
            true,
            0L,
            catalog.examples(),
            List.of()
        );
    }
}
