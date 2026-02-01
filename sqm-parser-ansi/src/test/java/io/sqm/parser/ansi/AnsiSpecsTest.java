package io.sqm.parser.ansi;

import io.sqm.core.dialect.SqlFeature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnsiSpecsTest {

    @Test
    void capabilities_expose_ansi_features() {
        var specs = new AnsiSpecs();
        assertTrue(specs.capabilities().supports(SqlFeature.DATE_TYPED_LITERAL));
        assertTrue(specs.capabilities().supports(SqlFeature.TIME_TYPED_LITERAL));
        assertTrue(specs.capabilities().supports(SqlFeature.TIMESTAMP_TYPED_LITERAL));
        assertTrue(specs.capabilities().supports(SqlFeature.BIT_STRING_LITERAL));
        assertTrue(specs.capabilities().supports(SqlFeature.HEX_STRING_LITERAL));
        assertTrue(specs.capabilities().supports(SqlFeature.IS_DISTINCT_FROM_PREDICATE));
        assertTrue(specs.capabilities().supports(SqlFeature.CUSTOM_OPERATOR));
        assertTrue(specs.capabilities().supports(SqlFeature.INTERVAL_LITERAL));
        assertFalse(specs.capabilities().supports(SqlFeature.DOLLAR_STRING_LITERAL));
        assertFalse(specs.capabilities().supports(SqlFeature.ESCAPE_STRING_LITERAL));
        assertFalse(specs.capabilities().supports(SqlFeature.ILIKE_PREDICATE));
        assertFalse(specs.capabilities().supports(SqlFeature.SIMILAR_TO_PREDICATE));
        assertFalse(specs.capabilities().supports(SqlFeature.DISTINCT_ON));
    }
}
