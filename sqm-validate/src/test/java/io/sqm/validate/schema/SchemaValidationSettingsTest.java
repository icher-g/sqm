package io.sqm.validate.schema;

import io.sqm.validate.schema.function.DefaultFunctionCatalog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaValidationSettingsTest {

    @Test
    void builder_normalizes_blank_principal_and_tenant_to_null() {
        var settings = SchemaValidationSettings.builder()
            .principal("   ")
            .tenant("")
            .build();

        assertNull(settings.principal());
        assertNull(settings.tenant());
        assertEquals(TenantRequirementMode.OPTIONAL, settings.tenantRequirementMode());
    }

    @Test
    void defaults_and_of_expose_expected_base_configuration() {
        var defaults = SchemaValidationSettings.defaults();
        var fromCatalog = SchemaValidationSettings.of(DefaultFunctionCatalog.standard());

        assertSame(DefaultFunctionCatalog.standard().getClass(), defaults.functionCatalog().getClass());
        assertSame(DefaultFunctionCatalog.standard().getClass(), fromCatalog.functionCatalog().getClass());
        assertTrue(defaults.additionalRules().isEmpty());
        assertTrue(fromCatalog.additionalRules().isEmpty());
    }
}

