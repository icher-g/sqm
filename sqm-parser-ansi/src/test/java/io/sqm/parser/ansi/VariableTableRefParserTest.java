package io.sqm.parser.ansi;

import io.sqm.core.VariableTableRef;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VariableTableRefParserTest {

    @Test
    void rejectsVariableTableParsingInAnsiDialect() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(VariableTableRef.class, "@audit");

        assertFalse(result.ok());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Variable tables are not supported"));
    }

    @Test
    void parserNeverMatchesAndExposesTargetType() {
        var parser = new VariableTableRefParser();
        var ctx = ParseContext.of(new AnsiSpecs());

        assertFalse(parser.match(Cursor.of("@audit", ctx.identifierQuoting()), ctx));
        assertEquals(VariableTableRef.class, parser.targetType());
    }
}
