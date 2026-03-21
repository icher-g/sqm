package io.sqm.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.sqm.core.Statement;
import io.sqm.json.SqmJsonMixins;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.sqlserver.spi.SqlServerDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class SqlServerDmlRoundTripIntegrationTest {

    private static final ObjectMapper MAPPER = SqmJsonMixins.createDefault()
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    private ParseContext parseContext;
    private RenderContext renderContext;

    private static String canonicalJson(Statement statement) {
        try {
            return MAPPER.writeValueAsString(statement);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    @BeforeEach
    void setUp() {
        parseContext = ParseContext.of(new SqlServerSpecs());
        renderContext = RenderContext.of(new SqlServerDialect());
    }

    @Test
    void roundTripInsertValuesStatement() {
        assertRoundTrip(
            "INSERT INTO [users] ([id], [name]) VALUES (1, 'alice'), (2, 'bob')",
            "INSERT INTO [users] ([id], [name]) VALUES (1, 'alice'), (2, 'bob')"
        );
    }

    @Test
    void roundTripUpdateStatement() {
        assertRoundTrip(
            "UPDATE [users] SET [name] = 'alice', [active] = 1 WHERE [id] = 1",
            "UPDATE [users] SET [name] = 'alice', [active] = 1 WHERE [id] = 1"
        );
    }

    @Test
    void roundTripInsertStatementWithOutputStar() {
        assertRoundTrip(
            "INSERT INTO [users] ([name]) OUTPUT inserted.* VALUES ('alice')",
            "INSERT INTO [users] ([name]) OUTPUT inserted.* VALUES ('alice')"
        );
    }

    @Test
    void roundTripUpdateStatementWithOutputInto() {
        assertRoundTrip(
            "UPDATE [users] SET [name] = 'alice' OUTPUT deleted.[name], inserted.[name] INTO [audit] ([old_name], [new_name]) WHERE [id] = 1",
            "UPDATE [users] SET [name] = 'alice' OUTPUT deleted.[name], inserted.[name] INTO [audit] ([old_name], [new_name]) WHERE [id] = 1"
        );
    }

    @Test
    void roundTripDeleteStatement() {
        assertRoundTrip(
            "DELETE FROM [users] WHERE [id] = 1",
            "DELETE FROM [users] WHERE [id] = 1"
        );
    }

    @Test
    void roundTripDeleteStatementWithOutputStar() {
        assertRoundTrip(
            "DELETE FROM [users] OUTPUT deleted.* WHERE [id] = 1",
            "DELETE FROM [users] OUTPUT deleted.* WHERE [id] = 1"
        );
    }

    @Test
    void roundTripMergeStatementWithTopOutputAndBySourceClause() {
        assertRoundTrip(
            """
                MERGE TOP (10) PERCENT INTO [users] WITH (HOLDLOCK)
                USING [src_users] AS [s]
                ON [users].[id] = [s].[id]
                WHEN MATCHED AND [s].[active] = 1 THEN UPDATE SET [name] = [s].[name]
                WHEN NOT MATCHED BY SOURCE AND [users].[active] = 0 THEN DELETE
                OUTPUT deleted.[id]
                """,
            """
                MERGE TOP (10) PERCENT INTO [users] WITH (HOLDLOCK)
                USING [src_users] AS [s]
                ON [users].[id] = [s].[id]
                WHEN MATCHED AND [s].[active] = 1 THEN UPDATE SET [name] = [s].[name]
                WHEN NOT MATCHED BY SOURCE AND [users].[active] = 0 THEN DELETE
                OUTPUT deleted.[id]
                """
        );
    }

    @Test
    void roundTripMergeStatementWithOutputInto() {
        assertRoundTrip(
            "MERGE INTO [users] USING [src] AS [s] ON [users].[id] = [s].[id] WHEN MATCHED THEN DELETE OUTPUT deleted.[id] INTO [audit] ([user_id])",
            "MERGE INTO [users] USING [src] AS [s] ON [users].[id] = [s].[id] WHEN MATCHED THEN DELETE OUTPUT deleted.[id] INTO [audit] ([user_id])"
        );
    }

    @Test
    void rejectsReturningForSqlServerBaseline() {
        var result = parseContext.parse(Statement.class, "INSERT INTO [users] ([id]) VALUES (1) RETURNING [id]");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("RETURNING"));
    }

    @Test
    void rejectsUpdateFromForSqlServerBaseline() {
        var result = parseContext.parse(
            Statement.class,
            "UPDATE [users] SET [name] = 'alice' FROM [users] INNER JOIN [orders] ON [users].[id] = [orders].[user_id]"
        );

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("UPDATE ... FROM"));
    }

    @Test
    void rejectsDeleteUsingForSqlServerBaseline() {
        var result = parseContext.parse(
            Statement.class,
            "DELETE FROM [users] USING [users] INNER JOIN [orders] ON [users].[id] = [orders].[user_id]"
        );

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("DELETE ... USING"));
    }

    @Test
    void ansiRendererRejectsSqlServerTopQueryModel() {
        var parsed = parseContext.parse(Statement.class, "SELECT TOP (5) [id] FROM [users]");
        assertTrue(parsed.ok(), parsed.errorMessage());

        var ansiRenderContext = RenderContext.of(new AnsiDialect());
        assertThrows(UnsupportedOperationException.class, () -> ansiRenderContext.render(parsed.value()));
    }

    private void assertRoundTrip(String originalSql, String expectedCanonicalSql) {
        var parsed = parseContext.parse(Statement.class, originalSql);
        assertTrue(parsed.ok(), parsed.errorMessage());

        var rendered = renderContext.render(parsed.value()).sql();
        assertEquals(normalize(expectedCanonicalSql), normalize(rendered));

        var reparsed = parseContext.parse(Statement.class, rendered);
        assertTrue(reparsed.ok(), reparsed.errorMessage());
        assertEquals(canonicalJson(parsed.value()), canonicalJson(reparsed.value()));
    }
}
