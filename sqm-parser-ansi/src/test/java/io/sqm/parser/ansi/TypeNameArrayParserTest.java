package io.sqm.parser.ansi;

import io.sqm.core.TimeZoneSpec;
import io.sqm.core.TypeName;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parser tests for PostgreSQL array type names.
 *
 * <p>These tests verify that {@link TypeName} parsing supports:</p>
 * <ul>
 *   <li>Array dimensions via {@code []}</li>
 *   <li>Multiple dimensions via {@code [][]}</li>
 *   <li>Modifiers together with arrays, e.g. {@code varchar(10)[]}</li>
 *   <li>Qualification together with arrays, e.g. {@code pg_catalog.int4[]}</li>
 * </ul>
 */
class TypeNameArrayParserTest {

    @Test
    void parsesArrayType_singleDim() {
        var t = parseTypeName("int[]");

        assertEquals(1, t.arrayDims());
        assertEquals(java.util.List.of("int"), t.qualifiedName());
        assertTrue(t.keyword().isEmpty());
        assertTrue(t.modifiers().isEmpty());
        assertEquals(TimeZoneSpec.NONE, t.timeZoneSpec());
    }

    @Test
    void parsesArrayType_multiDim() {
        var t = parseTypeName("text[][]");

        assertEquals(2, t.arrayDims());
        assertEquals(java.util.List.of("text"), t.qualifiedName());
    }

    @Test
    void parsesArrayType_withModifiers() {
        var t = parseTypeName("varchar(10)[]");

        assertEquals(java.util.List.of("varchar"), t.qualifiedName());
        assertEquals(1, t.modifiers().size());
        assertEquals(1, t.arrayDims());
    }

    @Test
    void parsesArrayType_withQualification() {
        var t = parseTypeName("pg_catalog.int4[]");

        assertEquals(java.util.List.of("pg_catalog", "int4"), t.qualifiedName());
        assertEquals(1, t.arrayDims());
    }

    @Test
    void parsesArrayType_withTimeZone_thenArray() {
        // PostgreSQL allows: timestamp with time zone[]
        // If your PgTypeNameParser supports TZ, this should pass.
        var t = parseTypeName("timestamp with time zone[]");

        assertEquals(java.util.List.of("timestamp"), t.qualifiedName());
        assertEquals(TimeZoneSpec.WITH_TIME_ZONE, t.timeZoneSpec());
        assertEquals(1, t.arrayDims());
    }

    @Test
    void rejectsNegativeDims_syntacticallyImpossibleButKeepTestAsGuard() {
        // This is mostly a guard that your parser doesn't accept weird tokens as dims.
        assertTypeParseError("int[[]");
    }

    // ---------------------------------------------------------------------
    // Harness hooks (wire to your existing PostgreSQL type-name parser entry points)
    // ---------------------------------------------------------------------

    private TypeName parseTypeName(String sql) {
        var ctx = ParseContext.of(new AnsiSpecs());
        return ctx.parse(TypeName.class, sql).value();
    }

    private void assertTypeParseError(String sql) {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(TypeName.class, sql);
        if (!result.isError()) {
            fail("Expected type-name parse error for: " + sql);
        }
    }
}
