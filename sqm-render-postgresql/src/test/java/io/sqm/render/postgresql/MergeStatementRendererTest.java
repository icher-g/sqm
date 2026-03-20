package io.sqm.render.postgresql;

import io.sqm.core.MergeClause;
import io.sqm.core.MergeDeleteAction;
import io.sqm.core.MergeInsertAction;
import io.sqm.core.MergeUpdateAction;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.MergeStatement;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.postgresql.spi.PostgresBooleans;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.postgresql.spi.PostgresIdentifierQuoter;
import io.sqm.render.postgresql.spi.PostgresNullSorting;
import io.sqm.render.postgresql.spi.PostgresOperators;
import io.sqm.render.postgresql.spi.PostgresPaginationStyle;
import io.sqm.render.postgresql.spi.PostgresValueFormatter;
import io.sqm.render.spi.Booleans;
import io.sqm.render.spi.IdentifierQuoter;
import io.sqm.render.spi.NullSorting;
import io.sqm.render.spi.Operators;
import io.sqm.render.spi.PaginationStyle;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.RenderersRepository;
import io.sqm.render.spi.SqlDialect;
import io.sqm.render.spi.ValueFormatter;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.merge;
import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.set;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MergeStatementRendererTest {

    @Test
    void rendersPostgresMergeFirstSliceWithReturning() {
        var ctx = RenderContext.of(new PostgresDialect(SqlDialectVersion.of(15, 0)));
        MergeStatement statement = merge("users")
            .source(tbl("src_users").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedUpdate(java.util.List.of(set("name", col("s", "name"))))
            .whenNotMatchedInsert(java.util.List.of(id("id"), id("name")), row(col("s", "id"), col("s", "name")))
            .result(col("id").toSelectItem())
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals(
            "MERGE INTO users USING src_users AS s ON users.id = s.id WHEN MATCHED THEN UPDATE SET name = s.name WHEN NOT MATCHED THEN INSERT (id, name) VALUES (s.id, s.name) RETURNING id",
            sql
        );
    }

    @Test
    void rejectsMergeBeforePostgres15() {
        var renderer = new MergeStatementRenderer();
        var ctx = RenderContext.of(new PostgresDialect(SqlDialectVersion.of(14, 0)));
        var writer = new DefaultSqlWriter(ctx);
        MergeStatement statement = merge("users")
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedDelete()
            .build();

        assertThrows(UnsupportedDialectFeatureException.class, () -> renderer.render(statement, ctx, writer));
    }

    @Test
    void rendersMergeWithoutReturning() {
        var ctx = RenderContext.of(new PostgresDialect(SqlDialectVersion.of(15, 0)));
        MergeStatement statement = merge("users")
            .source(tbl("src"))
            .on(col("users", "id").eq(col("src", "id")))
            .whenMatchedDelete()
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals(
            "MERGE INTO users USING src ON users.id = src.id WHEN MATCHED THEN DELETE",
            sql
        );
    }

    @Test
    void rejectsReturningWhenDialectDoesNotSupportIt() {
        var renderer = new MergeStatementRenderer();
        var ctx = RenderContext.of(new NoReturningPostgresDialect());
        var writer = new DefaultSqlWriter(ctx);
        MergeStatement statement = merge("users")
            .source(tbl("src"))
            .on(col("users", "id").eq(col("src", "id")))
            .whenMatchedDelete()
            .result(col("id").toSelectItem())
            .build();

        assertThrows(UnsupportedDialectFeatureException.class, () -> renderer.render(statement, ctx, writer));
    }

    @Test
    void rendersClauseAndActionLeaves() {
        var ctx = RenderContext.of(new PostgresDialect(SqlDialectVersion.of(15, 0)));

        var matchedDelete = normalize(ctx.render(MergeClause.of(MergeClause.MatchType.MATCHED, MergeDeleteAction.of())).sql());
        var notMatchedInsert = normalize(ctx.render(
            MergeClause.of(
                MergeClause.MatchType.NOT_MATCHED,
                MergeInsertAction.of(java.util.List.of(), row(lit(1), lit("alice")))
            )
        ).sql());
        var deleteAction = normalize(ctx.render(MergeDeleteAction.of()).sql());
        var updateAction = normalize(ctx.render(
            MergeUpdateAction.of(java.util.List.of(set("name", lit("alice")), set("email", lit("a@example.com"))))
        ).sql());
        var insertAction = normalize(ctx.render(
            MergeInsertAction.of(java.util.List.of(id("id"), id("name")), row(lit(1), lit("alice")))
        ).sql());

        assertEquals("WHEN MATCHED THEN DELETE", matchedDelete);
        assertEquals("WHEN NOT MATCHED THEN INSERT VALUES (1, 'alice')", notMatchedInsert);
        assertEquals("DELETE", deleteAction);
        assertEquals("UPDATE SET name = 'alice', email = 'a@example.com'", updateAction);
        assertEquals("INSERT (id, name) VALUES (1, 'alice')", insertAction);
    }

    @Test
    void skipsNullReturningClause() {
        var renderer = new MergeStatementRenderer();
        var ctx = RenderContext.of(new PostgresDialect(SqlDialectVersion.of(15, 0)));
        var writer = new DefaultSqlWriter(ctx);

        renderer.renderReturning(null, ctx, writer);

        assertEquals("", writer.toText(java.util.List.of()).sql());
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    private static final class NoReturningPostgresDialect implements SqlDialect {
        private final PostgresDialect delegate = new PostgresDialect(SqlDialectVersion.of(15, 0));
        private final ValueFormatter formatter = new PostgresValueFormatter(this);

        @Override
        public String name() {
            return delegate.name();
        }

        @Override
        public IdentifierQuoter quoter() {
            return new PostgresIdentifierQuoter();
        }

        @Override
        public ValueFormatter formatter() {
            return formatter;
        }

        @Override
        public Operators operators() {
            return new PostgresOperators();
        }

        @Override
        public Booleans booleans() {
            return new PostgresBooleans();
        }

        @Override
        public NullSorting nullSorting() {
            return new PostgresNullSorting();
        }

        @Override
        public PaginationStyle paginationStyle() {
            return new PostgresPaginationStyle();
        }

        @Override
        public DialectCapabilities capabilities() {
            return feature -> feature != SqlFeature.DML_RESULT_CLAUSE && delegate.capabilities().supports(feature);
        }

        @Override
        public RenderersRepository renderers() {
            return delegate.renderers();
        }
    }
}
