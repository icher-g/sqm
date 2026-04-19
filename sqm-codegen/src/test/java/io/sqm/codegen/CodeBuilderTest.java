package io.sqm.codegen;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CodeBuilderTest {

    @Test
    void commaHelpersHandleEmptyInlineAndMultilineLists() {
        var builder = CodeBuilder.of(2);

        assertSame(builder, builder.comma(null));
        assertSame(builder, builder.comma(List.of()));
        assertEquals("", builder.toString());

        builder.comma(List.of("a", "b"));
        assertEquals("a, b", builder.toString());

        builder.reset();
        builder.comma(List.of("a", "b"), true);
        assertEquals("a,\nb", builder.toString());

        builder.reset();
        builder.comma(List.of(1, 2), value -> builder.append("n").append(String.valueOf(value)));
        assertEquals("n1, n2", builder.toString());
    }

    @Test
    void quotesEscapesSpacingIndentAndResetAreStable() {
        var builder = CodeBuilder.of(2);

        builder.space()
            .append(null)
            .append("")
            .quote(null)
            .quote("")
            .escape(null)
            .escape("")
            .append("root")
            .space()
            .space()
            .quote("a\"b\\c")
            .space()
            .quote("x", '\'')
            .space()
            .escape("q\"r\\s")
            .nl()
            .in()
            .append("child\nnext")
            .out()
            .out()
            .nl()
            .space()
            .append("tail");

        assertEquals("root \"a\\\"b\\\\c\" 'x' q\\\"r\\\\s\n  child\n  next\ntail", builder.toString());

        builder.reset();
        builder.append("fresh");

        assertEquals("fresh", builder.toString());
    }

    @Test
    void zeroIndentBuilderDoesNotPadIndentedLines() {
        var builder = CodeBuilder.of(-1);

        builder.in().append("a\nb");

        assertEquals("a\nb", builder.toString());
    }

    @Test
    void spaceHandlesEmptyBufferAfterLineStartFlagWasCleared() throws ReflectiveOperationException {
        var builder = CodeBuilder.of();
        Field atLineStart = builder.getClass().getDeclaredField("atLineStart");
        atLineStart.setAccessible(true);
        atLineStart.set(builder, false);

        builder.space();

        assertEquals("", builder.toString());
    }
}
