package io.sqm.render.oracle;

import io.sqm.core.LimitOffset;
import io.sqm.core.MergeClause;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.core.dialect.VersionedDialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.oracle.spi.OracleDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.delete;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.insert;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.merge;
import static io.sqm.dsl.Dsl.param;
import static io.sqm.dsl.Dsl.resultVariableTarget;
import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.set;
import static io.sqm.dsl.Dsl.tbl;
import static io.sqm.dsl.Dsl.update;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OracleRenderSmokeTest {

    @Test
    void rendersSimpleQueryWithOracleDialect() {
        var query = select(lit(1L)).from(tbl("dual")).build();
        var sql = RenderContext.of(new OracleDialect()).render(query).sql();

        assertEquals("SELECT 1 FROM dual", sql.replaceAll("\\s+", " ").trim());
    }

    @Test
    void rendersFetchFirstForLimitOnlyQuery() {
        var query = select(lit(1L)).from(tbl("dual")).limit(lit(10L)).build();
        var sql = RenderContext.of(new OracleDialect()).render(query).sql();

        assertEquals("SELECT 1 FROM dual FETCH FIRST 10 ROWS ONLY", sql.replaceAll("\\s+", " ").trim());
    }

    @Test
    void rendersOffsetFetchForLimitAndOffsetQuery() {
        var query = select(lit(1L)).from(tbl("dual")).limit(lit(10L)).offset(lit(5L)).build();
        var sql = RenderContext.of(new OracleDialect()).render(query).sql();

        assertEquals("SELECT 1 FROM dual OFFSET 5 ROWS FETCH FIRST 10 ROWS ONLY", sql.replaceAll("\\s+", " ").trim());
    }

    @Test
    void rendersOracleOffsetOnlyAndEmptyLimitOffset() {
        var dialect = new OracleDialect();
        var offsetOnly = select(lit(1L)).from(tbl("dual")).offset(lit(5L)).build();
        var writer = new DefaultSqlWriter(RenderContext.of(dialect));

        new LimitOffsetRenderer().render(LimitOffset.of((io.sqm.core.Expression) null, null), RenderContext.of(dialect), writer);

        assertEquals("", writer.toText(java.util.List.of()).sql());
        assertEquals("SELECT 1 FROM dual OFFSET 5 ROWS", normalize(RenderContext.of(dialect).render(offsetOnly).sql()));
    }

    @Test
    void rejectsLimitAllForOracle() {
        var dialect = new OracleDialect();
        var writer = new DefaultSqlWriter(RenderContext.of(dialect));

        assertThrows(UnsupportedOperationException.class, () ->
            new LimitOffsetRenderer().render(LimitOffset.all(), RenderContext.of(dialect), writer)
        );
    }

    @Test
    void rendersLateralDerivedTableWithOracleCapabilities() {
        var inner = select(col("id")).from(tbl("orders")).build();
        var query = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .join(io.sqm.core.CrossJoin.of(tbl(inner).as("o").lateral()))
            .build();

        var sql = RenderContext.of(new OracleDialect()).render(query).sql();

        assertEquals(
            "SELECT u.id FROM users u CROSS JOIN LATERAL ( SELECT id FROM orders ) o",
            sql.replaceAll("\\s+", " ").trim()
        );
    }

    @Test
    void omitsAsForTableAliasesButRetainsItForColumnAliases() {
        var query = select(col("u", "id").as("user_id"))
            .from(tbl("users").as("u"))
            .build();

        assertEquals(
            "SELECT u.id AS user_id FROM users u",
            normalize(RenderContext.of(new OracleDialect()).render(query).sql())
        );
    }

    @Test
    void rendersBaselineOracleDml() {
        var dialect = new OracleDialect();
        var insert = insert("users").columns(id("id"), id("name")).values(row(lit(1L), lit("alice"))).build();
        var update = update("users").set("name", lit("alice")).where(col("id").eq(lit(1L))).build();
        var delete = delete("users").where(col("id").eq(lit(1L))).build();

        assertEquals("INSERT INTO users (id, name) VALUES (1, 'alice')", normalize(RenderContext.of(dialect).render(insert).sql()));
        assertEquals("UPDATE users SET name = 'alice' WHERE id = 1", normalize(RenderContext.of(dialect).render(update).sql()));
        assertEquals("DELETE FROM users WHERE id = 1", normalize(RenderContext.of(dialect).render(delete).sql()));
    }

    @Test
    void rendersOracleReturningIntoForDml() {
        var dialect = new OracleDialect();
        var insert = insert("users")
            .columns(id("id"), id("name"))
            .values(row(lit(1L), lit("alice")))
            .result(resultVariableTarget(param("id"), param("name")), col("id"), col("name"))
            .build();
        var update = update("users")
            .set("name", lit("alice"))
            .where(col("id").eq(lit(1L)))
            .result(resultVariableTarget(param(1)), col("id"))
            .build();
        var delete = delete("users")
            .where(col("id").eq(lit(1L)))
            .result(resultVariableTarget(param("id")), col("id"))
            .build();

        assertEquals("INSERT INTO users (id, name) VALUES (1, 'alice') RETURNING id, name INTO :id, :name", normalize(RenderContext.of(dialect).render(insert).sql()));
        assertEquals("UPDATE users SET name = 'alice' WHERE id = 1 RETURNING id INTO :1", normalize(RenderContext.of(dialect).render(update).sql()));
        assertEquals("DELETE FROM users WHERE id = 1 RETURNING id INTO :id", normalize(RenderContext.of(dialect).render(delete).sql()));
    }

    @Test
    void rejectsOracleReturningIntoWithoutVariableTargetsOrCapabilities() {
        var missingTarget = insert("users")
            .values(row(lit(1L)))
            .result(col("id"))
            .build();
        var unsupportedCapabilities = insert("users")
            .values(row(lit(1L)))
            .result(resultVariableTarget(param("id")), col("id"))
            .build();

        assertThrows(UnsupportedDialectFeatureException.class, () -> RenderContext.of(new OracleDialect()).render(missingTarget));
        assertThrows(UnsupportedDialectFeatureException.class, () -> RenderContext.of(new NoMergeOracleDialect()).render(unsupportedCapabilities));
    }

    @Test
    void skipsNullOracleReturningClause() {
        var ctx = RenderContext.of(new OracleDialect());
        var writer = new DefaultSqlWriter(ctx);

        OracleResultRendering.renderReturningInto("INSERT ... RETURNING INTO", null, ctx, writer);

        assertEquals("", writer.toText(java.util.List.of()).sql());
    }

    @Test
    void rendersBaselineOracleMerge() {
        var statement = merge("users")
            .source(tbl("src_users").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedUpdate(java.util.List.of(set("name", col("s", "name"))))
            .whenNotMatchedInsert(java.util.List.of(id("id"), id("name")), row(col("s", "id"), col("s", "name")))
            .build();

        var sql = RenderContext.of(new OracleDialect()).render(statement).sql();

        assertEquals(
            "MERGE INTO users USING src_users s ON (users.id = s.id) WHEN MATCHED THEN UPDATE SET name = s.name WHEN NOT MATCHED THEN INSERT (id, name) VALUES (s.id, s.name)",
            normalize(sql)
        );
    }

    @Test
    void rendersOracleOptimizerHintsAfterStatementVerbs() {
        var dialect = new OracleDialect();
        var select = select(col("id")).hint("FULL", id("users")).from(tbl("users")).build();
        var insert = insert("users").hint("APPEND").values(row(lit(1L))).build();
        var update = update("users").hint("FULL", id("users")).set("name", lit("alice")).build();
        var delete = delete("users").hint("FULL", id("users")).build();
        var merge = merge("users")
            .hint("MERGE")
            .source(tbl("src_users").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedUpdate(java.util.List.of(set("name", col("s", "name"))))
            .build();

        assertEquals("SELECT /*+ FULL(users) */ id FROM users", normalize(RenderContext.of(dialect).render(select).sql()));
        assertEquals("INSERT /*+ APPEND */ INTO users VALUES (1)", normalize(RenderContext.of(dialect).render(insert).sql()));
        assertEquals("UPDATE /*+ FULL(users) */ users SET name = 'alice'", normalize(RenderContext.of(dialect).render(update).sql()));
        assertEquals("DELETE /*+ FULL(users) */ FROM users", normalize(RenderContext.of(dialect).render(delete).sql()));
        assertEquals(
            "MERGE /*+ MERGE */ INTO users USING src_users s ON (users.id = s.id) WHEN MATCHED THEN UPDATE SET name = s.name",
            normalize(RenderContext.of(dialect).render(merge).sql())
        );
    }

    @Test
    void rejectsNonOracleMergeRenderShapes() {
        var top = merge("users")
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .top(io.sqm.core.TopSpec.of(lit(1L), false, false))
            .whenMatchedUpdate(java.util.List.of(set("name", col("s", "name"))))
            .build();
        var bySource = merge("users")
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .clause(MergeClause.of(MergeClause.MatchType.NOT_MATCHED_BY_SOURCE, null, io.sqm.core.MergeUpdateAction.of(java.util.List.of(set("name", col("s", "name"))))))
            .build();
        var returning = merge("users")
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedUpdate(java.util.List.of(set("name", col("s", "name"))))
            .result(col("id"))
            .build();
        var doNothing = merge("users")
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedDoNothing()
            .build();

        assertThrows(UnsupportedDialectFeatureException.class, () -> RenderContext.of(new OracleDialect()).render(top));
        assertThrows(UnsupportedDialectFeatureException.class, () -> RenderContext.of(new OracleDialect()).render(bySource));
        assertThrows(UnsupportedDialectFeatureException.class, () -> RenderContext.of(new OracleDialect()).render(returning));
        assertThrows(UnsupportedOperationException.class, () -> RenderContext.of(new OracleDialect()).render(doNothing));
        assertThrows(UnsupportedDialectFeatureException.class, () -> RenderContext.of(new NoMergeOracleDialect()).render(doNothing));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    private static final class NoMergeOracleDialect extends OracleDialect {
        /**
         * Returns an empty capability set so MERGE rendering takes the dialect feature rejection path.
         *
         * @return empty dialect capabilities
         */
        @Override
        public DialectCapabilities capabilities() {
            return VersionedDialectCapabilities.builder(SqlDialectVersion.of(19, 0)).build();
        }
    }
}
