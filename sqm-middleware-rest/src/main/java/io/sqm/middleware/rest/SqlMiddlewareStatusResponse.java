package io.sqm.middleware.rest;

/**
 * REST host status response for health/readiness diagnostics.
 *
 * @param status             transport status
 * @param schemaSource       configured schema source
 * @param schemaState        schema bootstrap state
 * @param schemaDescription  operator-facing schema source description
 * @param schemaErrorMessage optional schema bootstrap error message
 */
public record SqlMiddlewareStatusResponse(
    String status,
    String schemaSource,
    String schemaState,
    String schemaDescription,
    String schemaErrorMessage
) {
}

