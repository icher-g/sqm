package io.sqm.render.postgresql.spi;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PostgresValueFormatterTest {

    private final PostgresValueFormatter formatter = new PostgresValueFormatter(new PostgresDialect());

    @Test
    void formatsNull() {
        assertEquals("NULL", formatter.format(null));
    }

    @Test
    void formatsUuidLiteral() {
        var uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        assertEquals("'123e4567-e89b-12d3-a456-426614174000'::uuid", formatter.format(uuid));
    }

    @Test
    void formatsByteArrayAsBytea() {
        byte[] bytes = new byte[] {0x00, 0x0f, 0x10, (byte) 0xff};
        assertEquals("'\\x000f10ff'::bytea", formatter.format(bytes));
    }

    @Test
    void formatsByteBufferAsByteaWithoutConsuming() {
        byte[] bytes = new byte[] {0x01, 0x02, 0x03};
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.position(1);

        assertEquals("'\\x0203'::bytea", formatter.format(buffer));
        assertEquals(1, buffer.position());
    }

    @Test
    void delegatesToBaseForStringsAndNumbers() {
        assertEquals("'hi'", formatter.format("hi"));
        assertEquals("42", formatter.format(42));
        assertFalse(formatter.format(true).isEmpty());
    }
}
