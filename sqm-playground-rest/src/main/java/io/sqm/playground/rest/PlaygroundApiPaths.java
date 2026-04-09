package io.sqm.playground.rest;

/**
 * Shared route constants for the playground REST API.
 */
public final class PlaygroundApiPaths {

    /**
     * Base route prefix for all playground REST endpoints.
     */
    public static final String BASE_PATH = "/sqm/playground/api/v1";

    /**
     * MVC path pattern covering all playground REST endpoints.
     */
    public static final String MVC_PATTERN = BASE_PATH + "/**";

    /**
     * Servlet filter path pattern covering all playground REST endpoints.
     */
    public static final String FILTER_PATTERN = BASE_PATH + "/*";

    private PlaygroundApiPaths() {
    }
}
