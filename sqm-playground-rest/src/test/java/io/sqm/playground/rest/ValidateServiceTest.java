package io.sqm.playground.rest;

import io.sqm.playground.api.SqlDialectDto;
import io.sqm.playground.api.ValidateRequestDto;
import io.sqm.playground.rest.service.PlaygroundStatementSupport;
import io.sqm.playground.rest.service.ValidateService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests playground validate service behavior.
 */
class ValidateServiceTest {

    @Test
    void validateIgnoresUnknownSchemaObjectsForDialectOnlyValidation() {
        var service = new ValidateService(new PlaygroundStatementSupport());

        var response = service.validate(new ValidateRequestDto(
            "select missing_col from totally_unknown_table",
            SqlDialectDto.ansi
        ));

        assertTrue(response.success());
        assertTrue(response.valid());
        assertTrue(response.diagnostics().isEmpty());
    }

    @Test
    void validateReturnsDialectDiagnosticsForInvalidDialectSpecificQuery() {
        var service = new ValidateService(new PlaygroundStatementSupport());

        var response = service.validate(new ValidateRequestDto(
            "select distinct on (id) id, name from customer order by name",
            SqlDialectDto.postgresql
        ));

        assertTrue(response.success());
        assertFalse(response.valid());
        assertFalse(response.diagnostics().isEmpty());
        assertEquals("validate", response.diagnostics().getFirst().phase().name());
    }

    @Test
    void validateReturnsParseDiagnosticsForInvalidSql() {
        var service = new ValidateService(new PlaygroundStatementSupport());

        var response = service.validate(new ValidateRequestDto(
            "select from",
            SqlDialectDto.ansi
        ));

        assertFalse(response.success());
        assertFalse(response.valid());
        assertFalse(response.diagnostics().isEmpty());
        assertEquals("PARSE_ERROR", response.diagnostics().getFirst().code());
    }
}
