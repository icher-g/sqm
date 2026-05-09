package io.sqm.transpile.builtin;

import io.sqm.core.SelectQuery;
import io.sqm.core.WithQuery;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.parser.oracle.spi.OracleSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.transpile.DefaultSqlTranspiler;
import io.sqm.transpile.RewriteFidelity;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileOptions;
import io.sqm.transpile.TranspileStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.insert;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.*;

class HierarchicalQueryToRecursiveCteRuleTest {
    @Test
    void exposesRecursiveCteTargets() {
        var rule = new HierarchicalQueryToRecursiveCteRule();

        assertEquals("hierarchical-query-to-recursive-cte", rule.id());
        assertEquals(Set.of(SqlDialectId.ORACLE), rule.sourceDialects());
        assertEquals(Set.of(SqlDialectId.ANSI, SqlDialectId.MYSQL, SqlDialectId.POSTGRESQL), rule.targetDialects());
        assertTrue(rule.order() < 0);
    }

    @Test
    void rewritesSimpleConnectByToRecursiveCteAst() {
        var query = parse("""
            SELECT id, parent_id, LEVEL
            FROM categories
            START WITH parent_id IS NULL
            CONNECT BY PRIOR id = parent_id
            """);

        var result = new HierarchicalQueryToRecursiveCteRule()
            .apply(query, context(SqlDialectId.ORACLE, SqlDialectId.POSTGRESQL));

        assertTrue(result.changed());
        assertEquals(RewriteFidelity.EXACT, result.fidelity());
        var with = assertInstanceOf(WithQuery.class, result.statement());
        assertTrue(with.recursive());
        assertEquals(1, with.ctes().size());
        assertEquals("sqm_tree", with.ctes().getFirst().name().value());
        assertEquals(3, with.ctes().getFirst().columnAliases().size());
        assertNull(((SelectQuery) with.body()).hierarchical());
    }

    @Test
    void transpilerRendersRecursiveCteForPostgres() {
        var transpiler = SqlTranspilerBuilder.oracleToPostgres();

        var result = transpiler.transpile("""
            SELECT id, parent_id, LEVEL
            FROM categories
            START WITH parent_id IS NULL
            CONNECT BY PRIOR id = parent_id
            """);

        assertEquals(TranspileStatus.SUCCESS, result.status());
        var sql = normalizeSql(result.sql().orElseThrow());
        assertTrue(sql.contains("WITH RECURSIVE sqm_tree (id, parent_id, level) AS"));
        assertTrue(sql.contains("SELECT sqm_child.id AS id, sqm_child.parent_id AS parent_id, 1 AS level FROM categories AS sqm_child WHERE sqm_child.parent_id IS NULL"));
        assertTrue(sql.contains("UNION ALL"));
        assertTrue(sql.contains("INNER JOIN sqm_tree AS sqm_parent ON sqm_parent.id = sqm_child.parent_id"));
        assertTrue(sql.contains("SELECT sqm_tree.id, sqm_tree.parent_id, sqm_tree.level FROM sqm_tree"));
    }

    @Test
    void rejectsOrderSiblingsByBecauseRecursiveCteDoesNotPreserveSiblingOrderingYet() {
        var query = parse("""
            SELECT id, parent_id, LEVEL
            FROM categories
            START WITH parent_id IS NULL
            CONNECT BY PRIOR id = parent_id
            ORDER SIBLINGS BY name
            """);

        var result = new HierarchicalQueryToRecursiveCteRule()
            .apply(query, context(SqlDialectId.ORACLE, SqlDialectId.POSTGRESQL));

        assertFalse(result.changed());
        assertEquals(RewriteFidelity.UNSUPPORTED, result.fidelity());
        assertEquals("UNSUPPORTED_HIERARCHICAL_QUERY_REWRITE", result.problems().getFirst().code());
        assertTrue(result.problems().getFirst().message().contains("ORDER SIBLINGS BY"));
    }

    @Test
    void leavesStatementWithoutHierarchicalQueryUnchanged() {
        var query = select(col("id")).from(tbl("categories")).build();

        var result = new HierarchicalQueryToRecursiveCteRule()
            .apply(query, context(SqlDialectId.ORACLE, SqlDialectId.POSTGRESQL));

        assertFalse(result.changed());
        assertSame(query, result.statement());
        assertEquals("No hierarchical query detected", result.description());
    }

    @Test
    void leavesSelectWithOnlyNestedHierarchicalQueryUnchanged() {
        var inner = parse("""
            SELECT id
            FROM categories
            CONNECT BY PRIOR id = parent_id
            """);
        var outer = select(inner).build();

        var result = new HierarchicalQueryToRecursiveCteRule()
            .apply(outer, context(SqlDialectId.ORACLE, SqlDialectId.POSTGRESQL));

        assertFalse(result.changed());
        assertSame(outer, result.statement());
    }

    @Test
    void rejectsHierarchicalQueryInsideNonSelectStatement() {
        var query = parse("""
            SELECT id
            FROM categories
            CONNECT BY PRIOR id = parent_id
            """);
        var insert = insert(tbl("category_copy"))
            .columns(col("id").name())
            .query(query)
            .build();

        var result = new HierarchicalQueryToRecursiveCteRule()
            .apply(insert, context(SqlDialectId.ORACLE, SqlDialectId.POSTGRESQL));

        assertFalse(result.changed());
        assertEquals(RewriteFidelity.UNSUPPORTED, result.fidelity());
        assertEquals("UNSUPPORTED_HIERARCHICAL_QUERY_REWRITE", result.problems().getFirst().code());
        assertTrue(result.problems().getFirst().message().contains("top-level SELECT"));
    }

    private static SelectQuery parse(String sql) {
        var result = ParseContext.of(new OracleSpecs()).parse(SelectQuery.class, sql);
        assertTrue(result.ok(), result.errorMessage());
        return result.value();
    }

    private static TranspileContext context(SqlDialectId source, SqlDialectId target) {
        return new TranspileContext(source, target, TranspileOptions.defaults(), Optional.empty(), Optional.empty());
    }

    private static String normalizeSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    private static final class SqlTranspilerBuilder {
        private static io.sqm.transpile.SqlTranspiler oracleToPostgres() {
            return new DefaultSqlTranspiler.Builder()
                .sourceDialect(SqlDialectId.ORACLE)
                .targetDialect(SqlDialectId.POSTGRESQL)
                .build();
        }
    }
}
