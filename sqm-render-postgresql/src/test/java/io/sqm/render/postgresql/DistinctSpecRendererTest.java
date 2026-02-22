package io.sqm.render.postgresql;

import io.sqm.core.*;
import io.sqm.render.ansi.DistinctSpecRenderer;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PostgreSQL {@link DistinctSpecRenderer}.
 */
@DisplayName("PostgreSQL DistinctSpecRenderer Tests")
class DistinctSpecRendererTest {

    private RenderContext renderContext;

    @BeforeEach
    void setUp() {
        renderContext = RenderContext.of(new PostgresDialect());
    }

    @Test
    @DisplayName("Render simple DISTINCT")
    void rendersSimpleDistinct() {
        var query = Query.select(col("name"))
            .from(tbl("users"))
            .distinct(DistinctSpec.TRUE)
            .build();
        
        var sql = renderContext.render(query).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("SELECT DISTINCT name"));
    }

    @Test
    @DisplayName("Render DISTINCT with multiple columns")
    void rendersDistinctWithMultipleColumns() {
        var query = Query.select(col("name"), col("email"))
            .from(tbl("users"))
            .distinct(DistinctSpec.TRUE)
            .build();
        
        var sql = renderContext.render(query).sql();
        
        assertTrue(normalizeWhitespace(sql).startsWith("SELECT DISTINCT"));
    }

    @Test
    @DisplayName("Render DISTINCT with ORDER BY")
    void rendersDistinctWithOrderBy() {
        var query = Query.select(col("name"))
            .from(tbl("users"))
            .distinct(DistinctSpec.TRUE)
            .orderBy(order("name"))
            .build();
        
        var sql = renderContext.render(query).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("SELECT DISTINCT"));
        assertTrue(normalizeWhitespace(sql).contains("ORDER BY"));
    }

    @Test
    @DisplayName("Render DISTINCT with WHERE clause")
    void rendersDistinctWithWhere() {
        var query = Query.select(col("name"))
            .from(tbl("users"))
            .distinct(DistinctSpec.TRUE)
            .where(col("active").eq(lit(true)))
            .build();
        
        var sql = renderContext.render(query).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("SELECT DISTINCT"));
        assertTrue(normalizeWhitespace(sql).contains("WHERE"));
    }

    @Test
    @DisplayName("Render DISTINCT with aggregate function")
    void rendersDistinctWithAggregate() {
        var query = Query.select(func("COUNT", starArg()))
            .from(tbl("users"))
            .distinct(DistinctSpec.TRUE)
            .groupBy(group("department"))
            .build();
        
        var sql = renderContext.render(query).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("SELECT DISTINCT"));
        assertTrue(normalizeWhitespace(sql).contains("COUNT"));
    }

    @Test
    @DisplayName("Render DISTINCT with JOIN")
    void rendersDistinctWithJoin() {
        var query = Query.select(col("u", "name"))
            .from(tbl("users").as("u"))
            .join(inner(tbl("orders").as("o")).on(col("u", "id").eq(col("o", "user_id"))))
            .distinct(DistinctSpec.TRUE)
            .build();
        
        var sql = renderContext.render(query).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("SELECT DISTINCT"));
        assertTrue(normalizeWhitespace(sql).contains("JOIN"));
    }

    @Test
    @DisplayName("Render DISTINCT with subquery")
    void rendersDistinctWithSubquery() {
        var subquery = select(col("user_id"))
            .from(tbl("orders"))
            .distinct(DistinctSpec.TRUE)
            .build();
        
        var query = select(col("*"))
            .from(tbl("users"))
            .where(col("id").in(QueryExpr.of(subquery)))
            .build();
        
        var sql = renderContext.render(query).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("SELECT DISTINCT"));
        assertTrue(normalizeWhitespace(sql).contains("IN"));
    }

    private String normalizeWhitespace(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
