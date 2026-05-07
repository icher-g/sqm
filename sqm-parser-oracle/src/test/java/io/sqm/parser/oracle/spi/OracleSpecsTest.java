package io.sqm.parser.oracle.spi;

import io.sqm.core.DeleteStatement;
import io.sqm.core.InsertStatement;
import io.sqm.core.MergeClause;
import io.sqm.core.MergeStatement;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.Statement;
import io.sqm.core.UpdateStatement;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OracleSpecsTest {

    @Test
    void supports_double_quoted_identifiers() {
        var specs = new OracleSpecs();

        assertTrue(specs.identifierQuoting().supports('"'));
        assertFalse(specs.identifierQuoting().supports('['));
        assertFalse(specs.identifierQuoting().supports('`'));
    }

    @Test
    void exposes_oracle_capabilities_for_configured_version() {
        var specs = new OracleSpecs(SqlDialectVersion.of(11, 2));

        assertTrue(specs.capabilities().supports(SqlFeature.MERGE_STATEMENT));
        assertFalse(specs.capabilities().supports(SqlFeature.LATERAL));
    }

    @Test
    void parses_simple_query_with_oracle_specs() {
        var context = ParseContext.of(new OracleSpecs());
        var result = context.parse(Query.class, "SELECT \"u\".\"id\" FROM \"users\" AS \"u\"");

        assertFalse(result.isError());
        assertInstanceOf(Query.class, result.value());
    }

    @Test
    void parses_oracle_fetch_first_row_limiting() {
        var context = ParseContext.of(new OracleSpecs());
        var result = context.parse(Query.class, "SELECT id FROM users ORDER BY id FETCH FIRST 10 ROWS ONLY");

        assertFalse(result.isError());
        var query = assertInstanceOf(SelectQuery.class, result.value());
        assertNotNull(query.limitOffset().limit());
        assertNull(query.limitOffset().offset());
    }

    @Test
    void parses_oracle_offset_fetch_row_limiting() {
        var context = ParseContext.of(new OracleSpecs());
        var result = context.parse(Query.class, "SELECT id FROM users ORDER BY id OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY");

        assertFalse(result.isError());
        var query = assertInstanceOf(SelectQuery.class, result.value());
        assertNotNull(query.limitOffset().limit());
        assertNotNull(query.limitOffset().offset());
    }

    @Test
    void parses_oracle_offset_only_row_limiting() {
        var context = ParseContext.of(new OracleSpecs());
        var result = context.parse(Query.class, "SELECT id FROM users ORDER BY id OFFSET 5 ROWS");

        assertFalse(result.isError());
        var query = assertInstanceOf(SelectQuery.class, result.value());
        assertNull(query.limitOffset().limit());
        assertNotNull(query.limitOffset().offset());
    }

    @Test
    void rejects_limit_syntax_for_oracle() {
        var context = ParseContext.of(new OracleSpecs());
        var result = context.parse(Query.class, "SELECT id FROM users LIMIT 10");

        assertTrue(result.isError());
    }

    @Test
    void rejects_malformed_oracle_row_limiting() {
        var context = ParseContext.of(new OracleSpecs());

        assertTrue(context.parse(Query.class, "SELECT id FROM users OFFSET ROWS").isError());
        assertTrue(context.parse(Query.class, "SELECT id FROM users FETCH 10 ROWS ONLY").isError());
        assertTrue(context.parse(Query.class, "SELECT id FROM users FETCH FIRST 10 ONLY").isError());
        assertTrue(context.parse(Query.class, "SELECT id FROM users FETCH FIRST 10 ROWS").isError());
    }

    @Test
    void parses_lateral_derived_table_for_oracle() {
        var context = ParseContext.of(new OracleSpecs());
        var result = context.parse(
            Query.class,
            "SELECT * FROM users u JOIN LATERAL (SELECT id FROM orders WHERE user_id = u.id) o ON true"
        );

        assertFalse(result.isError());
        var query = assertInstanceOf(SelectQuery.class, result.value());
        assertNotNull(query.from());
        assertFalse(query.joins().isEmpty());
    }

    @Test
    void parses_baseline_oracle_dml() {
        var context = ParseContext.of(new OracleSpecs());

        assertTrue(context.parse(InsertStatement.class, "INSERT INTO users (id, name) VALUES (1, 'alice')").ok());
        assertTrue(context.parse(UpdateStatement.class, "UPDATE users SET name = 'alice' WHERE id = 1").ok());
        assertTrue(context.parse(DeleteStatement.class, "DELETE FROM users WHERE id = 1").ok());
        assertInstanceOf(Statement.class, context.parse(Statement.class, "UPDATE users SET name = 'alice'").value());
    }

    @Test
    void parses_baseline_oracle_merge() {
        var context = ParseContext.of(new OracleSpecs());
        var result = context.parse(
            MergeStatement.class,
            """
                MERGE INTO users
                USING src_users s
                ON users.id = s.id
                WHEN MATCHED THEN UPDATE SET name = s.name
                WHEN NOT MATCHED THEN INSERT (id, name) VALUES (s.id, s.name)
                """
        );

        assertTrue(result.ok(), result.errorMessage());
        assertEquals("users", result.value().target().name().value());
        assertEquals(2, result.value().clauses().size());
        assertTrue(result.value().clauses().stream().anyMatch(clause -> clause.matchType() == MergeClause.MatchType.MATCHED));
        assertTrue(result.value().clauses().stream().anyMatch(clause -> clause.matchType() == MergeClause.MatchType.NOT_MATCHED));
    }

    @Test
    void rejects_non_oracle_dml_result_clauses() {
        var context = ParseContext.of(new OracleSpecs());

        assertTrue(context.parse(InsertStatement.class, "INSERT INTO users (id) VALUES (1) RETURNING id").isError());
        assertTrue(context.parse(UpdateStatement.class, "UPDATE users SET name = 'alice' OUTPUT inserted.name").isError());
        assertTrue(context.parse(DeleteStatement.class, "DELETE FROM users OUTPUT deleted.id").isError());
    }

    @Test
    void rejects_non_oracle_merge_shapes() {
        var context = ParseContext.of(new OracleSpecs());

        assertTrue(context.parse(MergeStatement.class, "MERGE TOP (1) INTO users USING src ON users.id = src.id WHEN MATCHED THEN UPDATE SET name = src.name").isError());
        assertTrue(context.parse(MergeStatement.class, "MERGE INTO users USING src ON users.id = src.id WHEN NOT MATCHED BY SOURCE THEN UPDATE SET name = src.name").isError());
        assertTrue(context.parse(MergeStatement.class, "MERGE INTO users USING src ON users.id = src.id WHEN MATCHED THEN DO NOTHING").isError());
        assertTrue(context.parse(MergeStatement.class, "MERGE INTO users USING src ON users.id = src.id WHEN MATCHED THEN UPDATE SET name = src.name OUTPUT inserted.id").isError());
    }

    @Test
    void rejects_malformed_oracle_merge_shapes() {
        var context = ParseContext.of(new OracleSpecs());

        assertTrue(context.parse(MergeStatement.class, "MERGE INTO users USING src ON users.id = src.id").isError());
        assertTrue(context.parse(MergeStatement.class, "MERGE INTO users USING src ON users.id = src.id WHEN MATCHED THEN UPDATE SET name = src.name RETURNING id").isError());
        assertTrue(context.parse(MergeStatement.class, "MERGE INTO USING src ON users.id = src.id WHEN MATCHED THEN UPDATE SET name = src.name").isError());
        assertTrue(context.parse(MergeStatement.class, "MERGE INTO users USING ON users.id = src.id WHEN MATCHED THEN UPDATE SET name = src.name").isError());
        assertTrue(context.parse(MergeStatement.class, "MERGE INTO users USING src ON WHEN MATCHED THEN UPDATE SET name = src.name").isError());
    }
}
