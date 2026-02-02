package io.sqm.parser.ansi;

import io.sqm.core.FunctionTable;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FunctionTableParser}.
 * 
 * <p>Tests both feature rejection (ANSI) and actual parsing logic (TestSpecs).</p>
 */
@DisplayName("FunctionTableParser Tests")
class FunctionTableParserTest {

    private ParseContext ansiCtx;
    private ParseContext testCtx;
    private FunctionTableParser parser;

    @BeforeEach
    void setUp() {
        ansiCtx = ParseContext.of(new AnsiSpecs());
        testCtx = ParseContext.of(new TestSpecs());
        parser = new FunctionTableParser();
    }

    @Test
    @DisplayName("Parse simple function table is not supported in ANSI")
    void parseFunctionTableNotSupported() {
        var result = ansiCtx.parse(FunctionTable.class, "func()");

        assertFalse(result.ok());
        assertNotNull(result.errorMessage());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Function tables are not supported"));
    }

    @Test
    @DisplayName("Parse function table with arguments is not supported")
    void parseFunctionTableWithArgumentsNotSupported() {
        var result = ansiCtx.parse(FunctionTable.class, "func(1, 2, 3)");

        assertFalse(result.ok());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Function tables are not supported"));
    }

    @Test
    @DisplayName("Parse function table with alias is not supported")
    void parseFunctionTableWithAliasNotSupported() {
        var result = ansiCtx.parse(FunctionTable.class, "func() AS f");

        assertFalse(result.ok());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Function tables are not supported"));
    }

    @Test
    @DisplayName("Parse function table with column aliases is not supported")
    void parseFunctionTableWithColumnAliasesNotSupported() {
        var result = ansiCtx.parse(FunctionTable.class, "func() AS f(a, b)");

        assertFalse(result.ok());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Function tables are not supported"));
    }

    @Test
    @DisplayName("Parse function table WITH ORDINALITY is not supported")
    void parseFunctionTableWithOrdinalityNotSupported() {
        var result = ansiCtx.parse(FunctionTable.class, "func() WITH ORDINALITY");

        assertFalse(result.ok());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Function tables are not supported"));
    }

    @Test
    @DisplayName("Parse function table WITH ORDINALITY and alias is not supported")
    void parseFunctionTableWithOrdinalityAndAliasNotSupported() {
        var result = ansiCtx.parse(FunctionTable.class, "func() WITH ORDINALITY AS f");

        assertFalse(result.ok());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Function tables are not supported"));
    }

    @Test
    @DisplayName("Parse simple function table")
    void parseSimpleFunctionTable() {
        var result = testCtx.parse(FunctionTable.class, "generate_series(1, 10)");

        assertTrue(result.ok());
        var table = result.value();
        assertNotNull(table);
        assertNotNull(table.function());
        assertFalse(table.ordinality());
    }

    @Test
    @DisplayName("Parse function table with alias")
    void parseFunctionTableWithAlias() {
        var result = testCtx.parse(FunctionTable.class, "unnest(arr) AS t");

        assertTrue(result.ok());
        var table = result.value();
        assertNotNull(table);
        assertEquals("t", table.alias());
    }

    @Test
    @DisplayName("Parse function table with column aliases")
    void parseFunctionTableWithColumnAliases() {
        var result = testCtx.parse(FunctionTable.class, "func() AS f(col1, col2)");

        assertTrue(result.ok());
        var table = result.value();
        assertNotNull(table);
        assertEquals("f", table.alias());
        assertNotNull(table.columnAliases());
        assertEquals(2, table.columnAliases().size());
    }

    @Test
    @DisplayName("Parse function table WITH ORDINALITY")
    void parseFunctionTableWithOrdinality() {
        var result = testCtx.parse(FunctionTable.class, "unnest(array_col) WITH ORDINALITY");

        assertTrue(result.ok());
        var table = result.value();
        assertNotNull(table);
        assertTrue(table.ordinality());
    }

    @Test
    @DisplayName("Parse function table WITH ORDINALITY and alias")
    void parseFunctionTableWithOrdinalityAndAlias() {
        var result = testCtx.parse(FunctionTable.class, "generate_series(1, 5) WITH ORDINALITY AS t(val, ord)");

        assertTrue(result.ok());
        var table = result.value();
        assertNotNull(table);
        assertTrue(table.ordinality());
        assertEquals("t", table.alias());
        assertEquals(2, table.columnAliases().size());
    }

    @Test
    @DisplayName("Target type is FunctionTable")
    void targetTypeIsFunctionTable() {
        assertEquals(FunctionTable.class, parser.targetType());
    }
}
