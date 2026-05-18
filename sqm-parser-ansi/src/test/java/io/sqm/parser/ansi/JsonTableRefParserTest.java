package io.sqm.parser.ansi;

import io.sqm.core.JsonTableNestedPathColumn;
import io.sqm.core.JsonTableOrdinalityColumn;
import io.sqm.core.JsonTableRef;
import io.sqm.core.JsonTableScalarColumn;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class JsonTableRefParserTest {
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
        var table = assertInstanceOf(JsonTableRef.class, query.from());
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
}
