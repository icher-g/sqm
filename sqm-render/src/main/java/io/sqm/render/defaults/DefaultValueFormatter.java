package io.sqm.render.defaults;

import io.sqm.render.spi.SqlDialect;
import io.sqm.render.spi.ValueFormatter;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.stream.Collectors;

public record DefaultValueFormatter(SqlDialect dialect) implements ValueFormatter {

    private static final DateTimeFormatter TS_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter TS_MICROS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    @Override
    public String format(Object value) {
        switch (value) {
            case null -> {
                return "NULL";
            }
            case Number number -> {
                return number.toString();
            }
            case Boolean b -> {
                return b ? dialect.booleans().trueLiteral() : dialect.booleans().falseLiteral();
            }
            case LocalDate date -> {
                // ANSI typed literal
                return "DATE '" + date + "'";
            }
            case LocalTime time -> {
                return "TIME '" + time + "'";
            }
            case LocalDateTime dateTime -> {
                // Avoid LocalDateTime#toString() which uses 'T'
                var f = (dateTime.getNano() == 0) ? TS_SECONDS : TS_MICROS;
                return "TIMESTAMP '" + dateTime.format(f) + "'";
            }
            case OffsetDateTime odt -> {
                // ANSI standard (support varies by DB, but it's still a valid SQL literal form)
                return "TIMESTAMP WITH TIME ZONE '" + odt + "'";
            }
            case Instant instant -> {
                // Render as UTC timestamp-with-time-zone
                var odt = OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
                return "TIMESTAMP WITH TIME ZONE '" + odt + "'";
            }
            case CharSequence s -> {
                return "'" + escape(s.toString()) + "'";
            }
            case Character c -> {
                return "'" + escape(c.toString()) + "'";
            }
            case Collection<?> col -> {
                return col.stream()
                    .map(this::format)
                    .collect(Collectors.joining(", ", "(", ")"));
            }
            default -> throw new IllegalArgumentException("Unsupported literal type: " + value.getClass());
        }
    }

    private String escape(String s) {
        return s.replace("'", "''");
    }
}
