package io.sqm.render.ansi;

import io.sqm.render.SqlText;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class BaseValuesRendererTest {
    /**
     * Assert SQL + params using your BufferSqlWriter API
     */
    protected static void assertSqlAndParams(SqlText out, String expectedSql, List<Object> expectedParams) {
        assertEquals(expectedSql, out.sql().trim());
        assertEquals(expectedParams, out.params());
    }
}
