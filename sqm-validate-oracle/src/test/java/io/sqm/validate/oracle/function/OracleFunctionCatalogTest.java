package io.sqm.validate.oracle.function;

import io.sqm.catalog.model.CatalogType;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.validate.schema.function.FunctionArgKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OracleFunctionCatalogTest {

    @Test
    void resolve_usesFallbackForDefaultFunctions() {
        var catalog = OracleFunctionCatalog.of(SqlDialectVersion.of(19, 0));

        var signature = catalog.resolve("lower");

        assertTrue(signature.isPresent());
        assertEquals(CatalogType.STRING, signature.get().returnType().orElseThrow());
    }

    @Test
    void resolve_isCaseInsensitive() {
        var catalog = OracleFunctionCatalog.standard();

        var signature = catalog.resolve("NVL2");

        assertTrue(signature.isPresent());
        assertEquals(3, signature.get().minArity());
        assertEquals(3, signature.get().maxArity());
    }

    @Test
    void resolve_exposesOracleStringAndDateFunctions() {
        var catalog = OracleFunctionCatalog.standard();

        var substr = catalog.resolve("substr");
        var toDate = catalog.resolve("to_date");
        var sysdate = catalog.resolve("sysdate");

        assertTrue(substr.isPresent());
        assertTrue(toDate.isPresent());
        assertTrue(sysdate.isPresent());
        assertEquals(CatalogType.STRING, substr.get().returnType().orElseThrow());
        assertEquals(FunctionArgKind.STRING_EXPR, substr.get().argKinds().getFirst());
        assertEquals(CatalogType.DATE, toDate.get().returnType().orElseThrow());
        assertEquals(CatalogType.TIMESTAMP, sysdate.get().returnType().orElseThrow());
    }

    @Test
    void resolve_exposesOracleAggregateFunctions() {
        var catalog = OracleFunctionCatalog.standard();

        var listagg = catalog.resolve("listagg");
        var median = catalog.resolve("median");

        assertTrue(listagg.isPresent());
        assertTrue(median.isPresent());
        assertTrue(listagg.get().aggregate());
        assertTrue(median.get().aggregate());
        assertEquals(CatalogType.STRING, listagg.get().returnType().orElseThrow());
        assertEquals(CatalogType.DECIMAL, median.get().returnType().orElseThrow());
    }

    @Test
    void resolve_hidesVersionGatedFunctionsBeforeTheirMinimumVersion() {
        var catalog = OracleFunctionCatalog.of(SqlDialectVersion.of(9, 0));

        assertFalse(catalog.resolve("regexp_replace").isPresent());
        assertFalse(catalog.resolve("listagg").isPresent());
        assertFalse(catalog.resolve("json_value").isPresent());
    }

    @Test
    void resolve_exposesVersionGatedFunctionsFromTheirMinimumVersion() {
        var oracle10 = OracleFunctionCatalog.of(SqlDialectVersion.of(10, 0));
        var oracle112 = OracleFunctionCatalog.of(SqlDialectVersion.of(11, 2));
        var oracle121 = OracleFunctionCatalog.of(SqlDialectVersion.of(12, 1));

        assertTrue(oracle10.resolve("regexp_replace").isPresent());
        assertTrue(oracle112.resolve("listagg").isPresent());
        assertTrue(oracle121.resolve("json_value").isPresent());
    }

    @Test
    void resolve_returnsEmptyForNullName() {
        assertFalse(OracleFunctionCatalog.standard().resolve(null).isPresent());
    }
}
