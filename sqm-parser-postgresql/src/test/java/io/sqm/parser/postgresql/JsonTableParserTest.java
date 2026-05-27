package io.sqm.parser.postgresql;

import io.sqm.core.JsonTable;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class JsonTableParserTest {
    @Test
    void parsesJsonTableInPostgres17AndLater() {
        var ctx = ParseContext.of(new PostgresSpecs(SqlDialectVersion.of(17, 0)));

        var result = ctx.parse(Query.class, """
            SELECT *
            FROM JSON_TABLE(payload, '$.items[*]' COLUMNS (
              id NUMBER PATH '$.id',
              ord FOR ORDINALITY
            )) jt
            """);

        assertTrue(result.ok(), result::errorMessage);
        var query = assertInstanceOf(SelectQuery.class, result.value());
        var table = assertInstanceOf(JsonTable.class, query.from());
        assertEquals("$.items[*]", table.rootPath().text());
        assertEquals("jt", table.alias().value());
        assertEquals(2, table.columns().size());
    }

    @Test
    void rejectsJsonTableBeforePostgres17() {
        var ctx = ParseContext.of(new PostgresSpecs(SqlDialectVersion.of(16, 0)));

        var result = ctx.parse(Query.class, "SELECT * FROM JSON_TABLE(payload, '$' COLUMNS (id NUMBER PATH '$.id')) jt");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("JSON_TABLE is not supported"));
    }
}
