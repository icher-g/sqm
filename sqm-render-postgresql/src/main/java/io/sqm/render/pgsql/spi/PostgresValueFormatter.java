package io.sqm.render.pgsql.spi;

import io.sqm.render.defaults.DefaultValueFormatter;
import io.sqm.render.spi.SqlDialect;
import io.sqm.render.spi.ValueFormatter;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * PostgreSQL-specific {@link ValueFormatter}.
 * <p>
 * This formatter builds on the ANSI-oriented {@link DefaultValueFormatter} and adds PostgreSQL-specific
 * literal formats (e.g., {@code bytea} and {@code uuid}).
 * <p>
 * For all other value types, formatting is delegated to {@link DefaultValueFormatter}.
 */
public class PostgresValueFormatter implements ValueFormatter {

    private final DefaultValueFormatter base;

    public PostgresValueFormatter(SqlDialect dialect) {
        base = new DefaultValueFormatter(dialect);
    }

    private static String toHex(byte[] bytes) {
        final char[] digits = "0123456789abcdef".toCharArray();
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = digits[v >>> 4];
            out[i * 2 + 1] = digits[v & 0x0F];
        }
        return new String(out);
    }

    @Override
    public String format(Object value) {
        switch (value) {
            case null -> {
                return base.format(null);
            }
            case UUID uuid -> {
                // PostgreSQL UUID literal (explicit cast keeps the type unambiguous)
                return "'" + uuid + "'::uuid";
            }
            case byte[] bytes -> {
                // PostgreSQL bytea hex format: '\xDEADBEEF'::bytea
                return "'\\x" + toHex(bytes) + "'::bytea";
            }
            case ByteBuffer buf -> {
                // Use slice() so we don't mutate the original buffer's position/limit
                var dup = buf.slice();
                var bytes = new byte[dup.remaining()];
                dup.get(bytes);
                return "'\\x" + toHex(bytes) + "'::bytea";
            }
            default -> {
                return base.format(value);
            }
        }
    }
}
