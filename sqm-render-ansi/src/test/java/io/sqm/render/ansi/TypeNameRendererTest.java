package io.sqm.render.ansi; // adjust to your test package

import io.sqm.core.TimeZoneSpec;
import io.sqm.core.TypeKeyword;
import io.sqm.core.TypeName;
import io.sqm.core.QualifiedName;
import io.sqm.core.Identifier;
import io.sqm.core.QuoteStyle;
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
    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    @Test
    void rendersQualifiedNameWithDotDelimiter() {
        var node = TypeName.of(
            QualifiedName.of("pg_catalog", "int4"),
            null,
            List.of(),
            0,
            TimeZoneSpec.NONE
        );

        var w = new DefaultSqlWriter(ctx);
        renderer.render(node, ctx, w);

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

        var w = new DefaultSqlWriter(ctx);
        renderer.render(node, ctx, w);

        assertEquals("double precision", w.toText(List.of()).sql());
    }

    @Test
    void rendersModifiersInParentheses() {
        var node = TypeName.of(
            QualifiedName.of("numeric"),
            null,
            List.of(lit(10), lit(2)),
            0,
            TimeZoneSpec.NONE
        );

        var w = new DefaultSqlWriter(ctx);
        renderer.render(node, ctx, w);

        assertEquals("numeric(10, 2)", w.toText(List.of()).sql());
    }

    @Test
    void rendersArrayDimsInAnsiRenderer() {
        var node = TypeName.of(
            QualifiedName.of("text"),
            null,
            List.of(),
            2,
            TimeZoneSpec.NONE
        );

        var w = new DefaultSqlWriter(ctx);
        renderer.render(node, ctx, w);

        assertEquals("text[][]", w.toText(List.of()).sql());
    }

    @Test
    void rendersTimeZoneSpecInAnsiRenderer_withTimeZone() {
        var node = TypeName.of(
            QualifiedName.of("timestamp"),
            null,
            List.of(lit("3")),
            0,
            TimeZoneSpec.WITH_TIME_ZONE
        );

        var w = new DefaultSqlWriter(ctx);
        renderer.render(node, ctx, w);

        assertEquals("timestamp('3') with time zone", w.toText(List.of()).sql());
    }

    @Test
    void rendersTimeZoneSpecInAnsiRenderer_withoutTimeZone() {
        var node = TypeName.of(
            QualifiedName.of("timestamp"),
            null,
            List.of(),
            0,
            TimeZoneSpec.WITHOUT_TIME_ZONE
        );

        var w = new DefaultSqlWriter(ctx);
        renderer.render(node, ctx, w);

        assertEquals("timestamp without time zone", w.toText(List.of()).sql());
    }

    @Test
    void preserves_or_falls_back_quote_style_for_qualified_type_parts() {
        var node = TypeName.of(
            new QualifiedName(List.of(
                Identifier.of("pg_catalog", QuoteStyle.NONE),
                Identifier.of("Int4", QuoteStyle.DOUBLE_QUOTE)
            )),
            null,
            List.of(),
            0,
            TimeZoneSpec.NONE
        );

        var w = new DefaultSqlWriter(ctx);
        renderer.render(node, ctx, w);
        assertEquals("pg_catalog.\"Int4\"", w.toText(List.of()).sql());

        var fallback = TypeName.of(
            new QualifiedName(List.of(
                Identifier.of("pg_catalog", QuoteStyle.NONE),
                Identifier.of("Int4", QuoteStyle.BACKTICK)
            )),
            null,
            List.of(),
            0,
            TimeZoneSpec.NONE
        );
        var w2 = new DefaultSqlWriter(ctx);
        renderer.render(fallback, ctx, w2);
        assertEquals("pg_catalog.\"Int4\"", w2.toText(List.of()).sql());
    }
}
