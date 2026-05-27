package io.sqm.parser.ansi;

import io.sqm.core.JsonTableNestedPathColumn;
import io.sqm.core.JsonTableOrdinalityColumn;
import io.sqm.core.JsonTable;
import io.sqm.core.JsonTableScalarColumn;
import io.sqm.core.JsonTableExistsColumn;
import io.sqm.core.JsonTableBehavior;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class JsonTableParserTest {
    private final ParseContext enabled = ParseContext.of(new TestSpecs());
    private final ParseContext ansi = ParseContext.of(new AnsiSpecs());

    @Test
    void parsesJsonTableColumns() {
        var result = enabled.parse(Query.class, """
            SELECT *
            FROM JSON_TABLE(payload, '$.items[*]' COLUMNS (
              id NUMBER PATH '$.id' NULL ON EMPTY ERROR ON ERROR,
              ord FOR ORDINALITY,
              NESTED PATH '$.children[*]' COLUMNS (
                child_id NUMBER PATH '$.id'
              )
            )) jt
            """);

        assertTrue(result.ok(), result::errorMessage);
        var query = assertInstanceOf(SelectQuery.class, result.value());
        var table = assertInstanceOf(JsonTable.class, query.from());
        assertEquals("payload", table.json().matchExpression().column(c -> c.name().value()).orElse("missing"));
        assertEquals("$.items[*]", table.rootPath().text());
        assertEquals("jt", table.alias().value());
        assertInstanceOf(JsonTableScalarColumn.class, table.columns().getFirst());
        assertInstanceOf(JsonTableOrdinalityColumn.class, table.columns().get(1));
        assertInstanceOf(JsonTableNestedPathColumn.class, table.columns().get(2));
    }

    @Test
    void rejectsJsonTableWhenFeatureDisabled() {
        var result = ansi.parse(Query.class, "SELECT * FROM JSON_TABLE(payload, '$' COLUMNS (id NUMBER PATH '$.id')) jt");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("JSON_TABLE is not supported"));
    }

    @Test
    void parsesExistsColumnsWrappersAndSoftJsonTableKeywords() {
        var result = enabled.parse(Query.class, """
            SELECT *
            FROM JSON_TABLE(path, '$' columns (
              no_wrap NUMBER path '$.no' without wrapper empty on empty,
              wrap_col NUMBER path '$.wrap' with wrapper,
              conditional_col NUMBER path '$.conditional' with conditional wrapper,
              present BOOLEAN exists path '$.present' null on error
            )) jt
            """);

        assertTrue(result.ok(), result::errorMessage);
        var query = assertInstanceOf(SelectQuery.class, result.value());
        var table = assertInstanceOf(JsonTable.class, query.from());

        assertEquals("path", table.json().matchExpression().column(c -> c.name().value()).orElse("missing"));

        var noWrap = assertInstanceOf(JsonTableScalarColumn.class, table.columns().getFirst());
        assertEquals(JsonTableScalarColumn.Wrapper.WITHOUT, noWrap.wrapper());
        assertEquals(JsonTableBehavior.Kind.EMPTY, noWrap.onEmpty().kind());

        var wrap = assertInstanceOf(JsonTableScalarColumn.class, table.columns().get(1));
        assertEquals(JsonTableScalarColumn.Wrapper.WITH, wrap.wrapper());

        var conditional = assertInstanceOf(JsonTableScalarColumn.class, table.columns().get(2));
        assertEquals(JsonTableScalarColumn.Wrapper.CONDITIONAL, conditional.wrapper());

        var exists = assertInstanceOf(JsonTableExistsColumn.class, table.columns().get(3));
        assertEquals("present", exists.name().value());
        assertEquals(JsonTableBehavior.Kind.NULL, exists.onError().kind());
    }

    @Test
    void reportsTypeParserErrorsInsideJsonTableColumns() {
        var result = enabled.parse(Query.class, "SELECT * FROM JSON_TABLE(payload, '$' COLUMNS (id PATH '$.id')) jt");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Expected"));
    }
}
