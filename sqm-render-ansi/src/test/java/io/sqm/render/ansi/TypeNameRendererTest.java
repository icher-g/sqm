package io.sqm.render.ansi; // adjust to your test package

import io.sqm.core.TimeZoneSpec;
import io.sqm.core.TypeKeyword;
import io.sqm.core.TypeName;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.lit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ANSI TypeName rendering.
 *
 * <p>ANSI renderer is expected to support only:</p>
 * <ul>
 *   <li>qualified vs keyword form joining ({@code .} or space)</li>
 *   <li>type modifiers, e.g. {@code numeric(10,2)}</li>
 * </ul>
 *
 * <p>ANSI renderer is expected to reject:</p>
 * <ul>
 *   <li>PostgreSQL array type suffix {@code []}</li>
 *   <li>PostgreSQL time zone clause ({@code with/without time zone})</li>
 * </ul>
 */
public class TypeNameRendererTest {

    // Replace with your actual ANSI renderer class under test.
    private final TypeNameRenderer renderer = new TypeNameRenderer();

    @Test
    void rendersQualifiedNameWithDotDelimiter() {
        var node = TypeName.of(
            List.of("pg_catalog", "int4"),
            null,
            List.of(),
            0,
            TimeZoneSpec.NONE
        );

        var w = new DefaultSqlWriter(RenderContext.of(new AnsiDialect()));
        renderer.render(node, null, w);

        assertEquals("pg_catalog.int4", w.toText(List.of()).sql());
    }

    @Test
    void rendersKeywordNameWithSpaceDelimiter() {
        var node = TypeName.of(
            null,
            TypeKeyword.DOUBLE_PRECISION,
            List.of(),
            0,
            TimeZoneSpec.NONE
        );

        var w = new DefaultSqlWriter(RenderContext.of(new AnsiDialect()));
        renderer.render(node, null, w);

        assertEquals("double precision", w.toText(List.of()).sql());
    }

    @Test
    void rendersModifiersInParentheses() {
        var node = TypeName.of(
            List.of("numeric"),
            null,
            List.of(lit(10), lit(2)),
            0,
            TimeZoneSpec.NONE
        );

        var w = new DefaultSqlWriter(RenderContext.of(new AnsiDialect()));
        renderer.render(node, null, w);

        assertEquals("numeric(10, 2)", w.toText(List.of()).sql());
    }

    @Test
    void rendersArrayDimsInAnsiRenderer() {
        var node = TypeName.of(
            List.of("text"),
            null,
            List.of(),
            2,
            TimeZoneSpec.NONE
        );

        var w = new DefaultSqlWriter(RenderContext.of(new AnsiDialect()));
        renderer.render(node, null, w);

        assertEquals("text[][]", w.toText(List.of()).sql());
    }

    @Test
    void rendersTimeZoneSpecInAnsiRenderer_withTimeZone() {
        var node = TypeName.of(
            List.of("timestamp"),
            null,
            List.of(lit("3")),
            0,
            TimeZoneSpec.WITH_TIME_ZONE
        );

        var w = new DefaultSqlWriter(RenderContext.of(new AnsiDialect()));
        renderer.render(node, null, w);

        assertEquals("timestamp('3') with time zone", w.toText(List.of()).sql());
    }

    @Test
    void rendersTimeZoneSpecInAnsiRenderer_withoutTimeZone() {
        var node = TypeName.of(
            List.of("timestamp"),
            null,
            List.of(),
            0,
            TimeZoneSpec.WITHOUT_TIME_ZONE
        );

        var w = new DefaultSqlWriter(RenderContext.of(new AnsiDialect()));
        renderer.render(node, null, w);

        assertEquals("timestamp without time zone", w.toText(List.of()).sql());
    }
}
