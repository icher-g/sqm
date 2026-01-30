package io.sqm.render.defaults;

import io.sqm.render.RenderTestDialect;
import io.sqm.render.spi.ValueFormatter;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultValueFormatterTest {

    private final ValueFormatter formatter = new DefaultValueFormatter(new RenderTestDialect());

    @Test
    void formatsNullNumbersAndBooleans() {
        assertEquals("NULL", formatter.format(null));
        assertEquals("42", formatter.format(42));
        assertEquals("TRUE", formatter.format(true));
        assertEquals("FALSE", formatter.format(false));
    }

    @Test
    void formatsDatesAndTimes() {
        assertEquals("DATE '2025-01-02'", formatter.format(LocalDate.of(2025, 1, 2)));
        assertEquals("TIME '03:04:05'", formatter.format(LocalTime.of(3, 4, 5)));
        assertEquals("TIMESTAMP '2025-01-02 03:04:05'", formatter.format(LocalDateTime.of(2025, 1, 2, 3, 4, 5)));
        assertEquals("TIMESTAMP '2025-01-02 03:04:05.123456'",
            formatter.format(LocalDateTime.of(2025, 1, 2, 3, 4, 5, 123456000)));
    }

    @Test
    void formatsOffsetDateTimeAndInstant() {
        var odt = OffsetDateTime.of(2025, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC);
        assertEquals("TIMESTAMP WITH TIME ZONE '2025-01-02T03:04:05Z'", formatter.format(odt));

        var instant = Instant.parse("2025-01-02T03:04:05Z");
        assertEquals("TIMESTAMP WITH TIME ZONE '2025-01-02T03:04:05Z'", formatter.format(instant));
    }

    @Test
    void formatsStringsCharactersAndCollections() {
        assertEquals("'a''b'", formatter.format("a'b"));
        assertEquals("'x'", formatter.format('x'));
        assertEquals("(1, 'a')", formatter.format(List.of(1, "a")));
    }

    @Test
    void errorsOnUnsupportedType() {
        var ex = assertThrows(IllegalArgumentException.class, () -> formatter.format(new Object()));
        assertTrue(ex.getMessage().contains("Unsupported literal type"));
    }
}
