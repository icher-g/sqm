package io.sqm.catalog.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CatalogColumnAndTypeTest {
    @Test
    void column_requires_name_and_type() {
        assertThrows(NullPointerException.class, () -> CatalogColumn.of(null, CatalogType.STRING));
        assertThrows(NullPointerException.class, () -> CatalogColumn.of("id", null));
    }

    @Test
    void catalog_type_contains_expected_enum_values() {
        assertNotNull(CatalogType.valueOf("UNKNOWN"));
        assertNotNull(CatalogType.valueOf("STRING"));
        assertNotNull(CatalogType.valueOf("INTEGER"));
        assertNotNull(CatalogType.valueOf("LONG"));
        assertNotNull(CatalogType.valueOf("DECIMAL"));
        assertNotNull(CatalogType.valueOf("BOOLEAN"));
        assertNotNull(CatalogType.valueOf("UUID"));
        assertNotNull(CatalogType.valueOf("JSON"));
        assertNotNull(CatalogType.valueOf("JSONB"));
        assertNotNull(CatalogType.valueOf("BYTES"));
        assertNotNull(CatalogType.valueOf("ENUM"));
        assertNotNull(CatalogType.valueOf("DATE"));
        assertNotNull(CatalogType.valueOf("TIME"));
        assertNotNull(CatalogType.valueOf("TIMESTAMP"));
    }
}

