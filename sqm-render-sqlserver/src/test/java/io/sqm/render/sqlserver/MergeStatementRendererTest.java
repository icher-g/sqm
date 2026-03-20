package io.sqm.render.sqlserver;

import io.sqm.render.spi.RenderContext;
import io.sqm.render.sqlserver.spi.SqlServerDialect;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.inserted;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.merge;
import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.set;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MergeStatementRendererTest {

    @Test
    void rendersSqlServerMergeFirstSlice() {
        var mergeStatement = merge(tbl("users").withHoldLock())
            .source(tbl("src_users").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedUpdate(col("s", "active").eq(lit(1)), java.util.List.of(set("name", col("s", "name"))))
            .whenNotMatchedInsert(col("s", "name").isNotNull(), java.util.List.of(id("id"), id("name")), row(col("s", "id"), col("s", "name")))
            .build();

        var rendered = RenderContext.of(new SqlServerDialect()).render(mergeStatement);

        assertEquals(
            "MERGE INTO users WITH (HOLDLOCK) USING src_users AS s ON users.id = s.id WHEN MATCHED AND s.active = 1 THEN UPDATE SET name = s.name WHEN NOT MATCHED AND s.name IS NOT NULL THEN INSERT (id, name) VALUES (s.id, s.name)",
            normalize(rendered.sql())
        );
    }

    @Test
    void acceptsTwoMatchedClausesWithFirstPredicateAndDistinctActions() {
        var mergeStatement = merge("users")
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedDelete(col("s", "active").eq(lit(1)))
            .whenMatchedUpdate(java.util.List.of(set("name", col("s", "name"))))
            .build();

        var rendered = RenderContext.of(new SqlServerDialect()).render(mergeStatement);

        assertEquals(
            "MERGE INTO users USING src AS s ON users.id = s.id WHEN MATCHED AND s.active = 1 THEN DELETE WHEN MATCHED THEN UPDATE SET name = s.name",
            normalize(rendered.sql())
        );
    }

    @Test
    void rejectsTwoMatchedClausesWithoutPredicateOnFirstClause() {
        var mergeStatement = merge("users")
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedUpdate(java.util.List.of(set("name", col("s", "name"))))
            .whenMatchedDelete()
            .build();

        assertThrows(UnsupportedOperationException.class, () -> RenderContext.of(new SqlServerDialect()).render(mergeStatement));
    }

    @Test
    void rejectsTwoMatchedClausesWithSameActionFamily() {
        var mergeStatement = merge("users")
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedUpdate(col("s", "active").eq(lit(1)), java.util.List.of(set("name", col("s", "name"))))
            .whenMatchedUpdate(java.util.List.of(set("name", col("s", "other_name"))))
            .build();

        assertThrows(UnsupportedOperationException.class, () -> RenderContext.of(new SqlServerDialect()).render(mergeStatement));
    }

    @Test
    void rejectsMoreThanTwoMatchedClauses() {
        var mergeStatement = merge("users")
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedUpdate(col("s", "active").eq(lit(1)), java.util.List.of(set("name", col("s", "name"))))
            .whenMatchedDelete()
            .whenMatchedDelete()
            .build();

        assertThrows(UnsupportedOperationException.class, () -> RenderContext.of(new SqlServerDialect()).render(mergeStatement));
    }

    @Test
    void rejectsDuplicateNotMatchedInsertClauses() {
        var mergeStatement = merge("users")
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenNotMatchedInsert(java.util.List.of(id("id")), row(col("s", "id")))
            .whenNotMatchedInsert(java.util.List.of(id("id")), row(col("s", "id")))
            .build();

        assertThrows(UnsupportedOperationException.class, () -> RenderContext.of(new SqlServerDialect()).render(mergeStatement));
    }

    @Test
    void rejectsMergeOutputInFirstSlice() {
        var mergeStatement = merge("users")
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedDelete()
            .result(inserted("id"))
            .build();

        assertThrows(UnsupportedOperationException.class, () -> RenderContext.of(new SqlServerDialect()).render(mergeStatement));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
