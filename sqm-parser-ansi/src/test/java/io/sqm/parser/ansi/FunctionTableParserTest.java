package io.sqm.parser.ansi;

import io.sqm.core.FunctionTable;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FunctionTableParser} in ANSI dialect.
 * 
 * <p>Note: ANSI SQL does not support function tables, so all tests should fail.</p>
 */
@DisplayName("ANSI FunctionTableParser Tests")
class FunctionTableParserTest {

    private ParseContext ctx;
    private FunctionTableParser parser;

    @BeforeEach
    void setUp() {
        ctx = ParseContext.of(new AnsiSpecs());
        parser = new FunctionTableParser();
    }

    @Test
    @DisplayName("Parse simple function table is not supported in ANSI")
    void parseFunctionTableNotSupported() {
        var result = ctx.parse(FunctionTable.class, "func()");

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Function tables are not supported"));
    }

    @Test
    @DisplayName("Parse function table with arguments is not supported")
    void parseFunctionTableWithArgumentsNotSupported() {
        var result = ctx.parse(FunctionTable.class, "func(1, 2, 3)");

        assertFalse(result.ok());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Function tables are not supported"));
    }

    @Test
    @DisplayName("Parse function table with alias is not supported")
    void parseFunctionTableWithAliasNotSupported() {
        var result = ctx.parse(FunctionTable.class, "func() AS f");

        assertFalse(result.ok());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Function tables are not supported"));
    }

    @Test
    @DisplayName("Parse function table with column aliases is not supported")
    void parseFunctionTableWithColumnAliasesNotSupported() {
        var result = ctx.parse(FunctionTable.class, "func() AS f(a, b)");

        assertFalse(result.ok());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Function tables are not supported"));
    }

    @Test
    @DisplayName("Parse function table WITH ORDINALITY is not supported")
    void parseFunctionTableWithOrdinalityNotSupported() {
        var result = ctx.parse(FunctionTable.class, "func() WITH ORDINALITY");

        assertFalse(result.ok());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Function tables are not supported"));
    }

    @Test
    @DisplayName("Parse function table WITH ORDINALITY and alias is not supported")
    void parseFunctionTableWithOrdinalityAndAliasNotSupported() {
        var result = ctx.parse(FunctionTable.class, "func() WITH ORDINALITY AS f");

        assertFalse(result.ok());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Function tables are not supported"));
    }

    @Test
    @DisplayName("Parse function table WITH without ORDINALITY is not supported")
    void parseFunctionTableWithWithoutOrdinalityNotSupported() {
        var result = ctx.parse(FunctionTable.class, "func() WITH");

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
    }

    @Test
    @DisplayName("Target type is FunctionTable")
    void targetTypeIsFunctionTable() {
        assertEquals(FunctionTable.class, parser.targetType());
    }

    @Test
    @DisplayName("Match returns true for function call syntax")
    void matchReturnsTrueForFunctionCallSyntax() {
        var cur = io.sqm.parser.core.Cursor.of("func()", ctx.identifierQuoting());
        assertTrue(parser.match(cur, ctx));
    }

    @Test
    @DisplayName("Match returns false for non-function syntax")
    void matchReturnsFalseForNonFunctionSyntax() {
        var cur = io.sqm.parser.core.Cursor.of("table1", ctx.identifierQuoting());
        assertFalse(parser.match(cur, ctx));
    }
}
