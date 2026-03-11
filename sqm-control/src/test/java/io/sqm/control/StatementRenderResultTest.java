package io.sqm.control;

import io.sqm.control.pipeline.StatementRenderResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatementRenderResultTest {

    @Test
    void factories_create_expected_results() {
        var sqlOnly = StatementRenderResult.of("select 1");
        var withParams = StatementRenderResult.of("select ?", List.of(1L));

        assertEquals("select 1", sqlOnly.sql());
        assertTrue(sqlOnly.params().isEmpty());

        assertEquals("select ?", withParams.sql());
        assertEquals(List.of(1L), withParams.params());
    }
}
