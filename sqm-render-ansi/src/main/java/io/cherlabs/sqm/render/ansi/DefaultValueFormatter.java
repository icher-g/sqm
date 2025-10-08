package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.render.spi.SqlDialect;
import io.cherlabs.sqm.render.spi.ValueFormatter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.stream.Collectors;

public record DefaultValueFormatter(SqlDialect dialect) implements ValueFormatter {

    @Override
    public String format(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Boolean b) {
            if (b) return dialect.booleans().trueLiteral();
            return dialect.booleans().falseLiteral();
        }
        if (value instanceof LocalDate date) {
            return "'" + date + "'"; // ISO format
        }
        if (value instanceof LocalDateTime dateTime) {
            return "'" + dateTime + "'";
        }
        if (value instanceof CharSequence s) {
            return "'" + escape(s.toString()) + "'";
        }
        if (value instanceof Character c) {
            return "'" + escape(c.toString()) + "'";
        }
        if (value instanceof Collection<?> col) {
            return col.stream()
                    .map(this::format)
                    .collect(Collectors.joining(", ", "(", ")"));
        }
        throw new IllegalArgumentException("Unsupported literal type: " + value.getClass());
    }

    private String escape(String s) {
        // SQL string literal escaping: double up single quotes
        return s.replace("'", "''");
    }
}

