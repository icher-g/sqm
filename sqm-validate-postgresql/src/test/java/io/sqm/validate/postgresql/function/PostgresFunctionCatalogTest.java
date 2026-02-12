package io.sqm.validate.postgresql.function;

import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.validate.schema.function.FunctionArgKind;
import io.sqm.validate.schema.model.DbType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgresFunctionCatalogTest {

    @Test
    void resolve_usesFallbackForDefaultFunctions() {
        var catalog = PostgresFunctionCatalog.of(SqlDialectVersion.of(9, 0));

        var signature = catalog.resolve("lower");

        assertTrue(signature.isPresent());
        assertEquals(DbType.STRING, signature.get().returnType().orElseThrow());
    }

    @Test
    void resolve_isCaseInsensitive() {
        var catalog = PostgresFunctionCatalog.of(SqlDialectVersion.of(12, 0));

        var signature = catalog.resolve("JSONB_TYPEOF");

        assertTrue(signature.isPresent());
        assertEquals(DbType.STRING, signature.get().returnType().orElseThrow());
    }

    @Test
    void resolve_hidesJsonbFunctionsBefore94() {
        var catalog = PostgresFunctionCatalog.of(SqlDialectVersion.of(9, 0));

        assertFalse(catalog.resolve("to_jsonb").isPresent());
        assertFalse(catalog.resolve("jsonb_build_object").isPresent());
        assertFalse(catalog.resolve("json_build_object").isPresent());
    }

    @Test
    void resolve_exposesJsonbFunctionsFrom94() {
        var catalog = PostgresFunctionCatalog.of(SqlDialectVersion.of(9, 4));

        var toJsonb = catalog.resolve("to_jsonb");
        var jsonbBuildObject = catalog.resolve("jsonb_build_object");
        var jsonBuildObject = catalog.resolve("json_build_object");

        assertTrue(toJsonb.isPresent());
        assertTrue(jsonbBuildObject.isPresent());
        assertTrue(jsonBuildObject.isPresent());
        assertEquals(DbType.JSONB, toJsonb.get().returnType().orElseThrow());
        assertEquals(DbType.JSONB, jsonbBuildObject.get().returnType().orElseThrow());
        assertEquals(DbType.JSON, jsonBuildObject.get().returnType().orElseThrow());
    }

    @Test
    void resolve_overridesStringAggReturnTypeToString() {
        var catalog = PostgresFunctionCatalog.standard();

        var signature = catalog.resolve("string_agg");

        assertTrue(signature.isPresent());
        assertEquals(DbType.STRING, signature.get().returnType().orElseThrow());
    }

    @Test
    void resolve_hidesJsonTypeofBefore92() {
        var catalog = PostgresFunctionCatalog.of(SqlDialectVersion.of(9, 1));

        assertFalse(catalog.resolve("json_typeof").isPresent());
    }

    @Test
    void resolve_exposesJsonTypeofFrom92() {
        var catalog = PostgresFunctionCatalog.of(SqlDialectVersion.of(9, 2));

        var signature = catalog.resolve("json_typeof");

        assertTrue(signature.isPresent());
        assertEquals(DbType.STRING, signature.get().returnType().orElseThrow());
    }

    @Test
    void resolve_hidesFormatBefore91() {
        var catalog = PostgresFunctionCatalog.of(SqlDialectVersion.of(9, 0));

        assertFalse(catalog.resolve("format").isPresent());
    }

    @Test
    void resolve_exposesFormatFrom91WithVariadicTail() {
        var catalog = PostgresFunctionCatalog.of(SqlDialectVersion.of(9, 1));

        var signature = catalog.resolve("format");

        assertTrue(signature.isPresent());
        assertEquals(1, signature.get().minArity());
        assertEquals(Integer.MAX_VALUE, signature.get().maxArity());
        assertEquals(DbType.STRING, signature.get().returnType().orElseThrow());
        assertEquals(FunctionArgKind.STRING_EXPR, signature.get().argKinds().getFirst());
        assertEquals(FunctionArgKind.ANY_EXPR, signature.get().argKinds().get(1));
    }

    @Test
    void resolve_exposesArrayLengthAsInteger() {
        var catalog = PostgresFunctionCatalog.of(SqlDialectVersion.of(12, 0));

        var signature = catalog.resolve("array_length");

        assertTrue(signature.isPresent());
        assertEquals(DbType.INTEGER, signature.get().returnType().orElseThrow());
        assertEquals(2, signature.get().minArity());
        assertEquals(2, signature.get().maxArity());
    }
}
