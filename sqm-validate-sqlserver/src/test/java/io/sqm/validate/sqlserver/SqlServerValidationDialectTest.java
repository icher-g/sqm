package io.sqm.validate.sqlserver;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.core.Identifier;
import io.sqm.core.LimitOffset;
import io.sqm.core.SelectModifier;
import io.sqm.core.Table;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.SchemaStatementValidator;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlServerValidationDialectTest {
    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "users",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("name", CatalogType.STRING),
            CatalogColumn.of("payload", CatalogType.BYTES),
            CatalogColumn.of("created_at", CatalogType.TIMESTAMP),
            CatalogColumn.of("updated_at", CatalogType.TIMESTAMP),
            CatalogColumn.of("age", CatalogType.INTEGER)
        )
    );

    @Test
    void validate_acceptsBaselineSqlServerSelectWithTop() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var query = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .top(10)
            .build();

        var result = validator.validate(query);

        assertFalse(hasDialectProblem(result, ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED));
        assertFalse(hasDialectProblem(result, ValidationProblem.Code.DIALECT_CLAUSE_INVALID));
    }

    @Test
    void validate_acceptsBaselineSqlServerSelectWithOffsetFetch() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var query = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .orderBy(order(col("u", "id")))
            .limitOffset(LimitOffset.of(lit(5L), lit(10L)))
            .build();

        var result = validator.validate(query);

        assertFalse(hasDialectProblem(result, ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED));
        assertFalse(hasDialectProblem(result, ValidationProblem.Code.DIALECT_CLAUSE_INVALID));
    }

    @Test
    void validate_reportsTopAndOffsetFetchCombination() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var query = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .orderBy(order(col("u", "id")))
            .top(10)
            .limitOffset(LimitOffset.of(lit(5L), lit(10L)))
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "limit_offset".equals(problem.clausePath())
                && problem.message().contains("TOP")
        ));
    }

    @Test
    void validate_reportsOffsetFetchWithoutOrderBy() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var query = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .limitOffset(LimitOffset.of(lit(5L), lit(10L)))
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "limit_offset".equals(problem.clausePath())
                && problem.message().contains("ORDER BY")
        ));
    }

    @Test
    void validate_reportsLimitOnlyWithoutTop() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var query = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .limit(10)
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "limit_offset".equals(problem.clausePath())
                && problem.message().contains("TOP")
        ));
    }

    @Test
    void validate_reportsDeferredTopVariants() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var topPercentQuery = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .top(topPercent(lit(10L)))
            .build();
        var topWithTiesQuery = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .top(topWithTies(lit(10L)))
            .build();

        var topPercentResult = validator.validate(topPercentQuery);
        var topWithTiesResult = validator.validate(topWithTiesQuery);

        assertTrue(topPercentResult.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "select.top".equals(problem.clausePath())
                && problem.message().contains("PERCENT")
        ));
        assertTrue(topWithTiesResult.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "select.top".equals(problem.clausePath())
                && problem.message().contains("WITH TIES")
        ));
    }

    @Test
    void validate_reportsUnsupportedSqlServerSelectExtensions() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var query = select(col("u", "id"))
            .from(tbl("users").as("u").withIndexHints(java.util.List.of(
                Table.IndexHint.use(Table.IndexHintScope.DEFAULT, java.util.List.of(Identifier.of("idx_users_name")))
            )))
            .selectModifier(SelectModifier.CALC_FOUND_ROWS)
            .optimizerHint("INDEX(users idx_users_name)")
            .orderBy(order(col("u", "id")).using(">"))
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "select.modifier".equals(problem.clausePath())
        ));
        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "select.hint".equals(problem.clausePath())
        ));
        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "select.order".equals(problem.clausePath())
        ));
        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "from.table".equals(problem.clausePath())
        ));
    }

    @Test
    void validate_reportsLockingAndLimitAllAsUnsupported() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var query = select(col("u", "id"))
            .from(tbl("users").as("u").only())
            .orderBy(order(col("u", "id")))
            .limitOffset(limitAll())
            .lockFor(update(), ofTables("u"), false, false)
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "select.lock".equals(problem.clausePath())
        ));
        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "limit_offset".equals(problem.clausePath())
                && problem.message().contains("LIMIT ALL")
        ));
        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "from.table".equals(problem.clausePath())
                && problem.message().contains("inheritance")
        ));
    }

    @Test
    void validate_acceptsBaselineSqlServerDmlAndRetainsBaseRules() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var insertStatement = insert("users")
            .columns(id("id"), id("name"))
            .values(rows(row(lit(1L), lit("alice"))))
            .build();
        var updateStatement = update("users")
            .set(Identifier.of("missing_col"), lit("alice"))
            .build();
        var deleteStatement = delete("users")
            .where(col("id").eq(lit(1L)))
            .build();

        var insertResult = validator.validate(insertStatement);
        var updateResult = validator.validate(updateStatement);
        var deleteResult = validator.validate(deleteStatement);

        assertFalse(hasDialectProblem(insertResult, ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED));
        assertTrue(updateResult.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.COLUMN_NOT_FOUND
                && "dml.assignment".equals(problem.clausePath())
        ));
        assertFalse(hasDialectProblem(deleteResult, ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED));
    }

    @Test
    void validate_acceptsFirstWaveSqlServerFunctions() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var scalarQuery = select(
            len(col("u", "name")),
            dataLength(col("u", "payload")),
            getDate(),
            dateAdd("day", lit(1), col("u", "created_at")),
            dateDiff("day", col("u", "created_at"), col("u", "updated_at")),
            isNullFn(col("u", "name"), lit("unknown"))
        )
            .from(tbl("users").as("u"))
            .where(getDate().gte(col("u", "created_at")))
            .build();
        var aggregateQuery = select(
            col("u", "age"),
            stringAgg(col("u", "name"), lit(",")).withinGroup(orderBy(order(col("u", "name"))))
        )
            .from(tbl("users").as("u"))
            .groupBy(group(col("u", "age")))
            .build();

        var scalarResult = validator.validate(scalarQuery);
        var aggregateResult = validator.validate(aggregateQuery);

        assertTrue(scalarResult.ok(), scalarResult.problems().toString());
        assertTrue(aggregateResult.ok(), aggregateResult.problems().toString());
    }

    @Test
    void validate_reportsSqlServerFunctionSignatureMismatch() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var query = select(
            len(col("u", "age")),
            dateAdd("day", col("u", "name"), col("u", "created_at"))
        )
            .from(tbl("users").as("u"))
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.FUNCTION_SIGNATURE_MISMATCH
                && problem.message().contains("LEN")
        ));
        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.FUNCTION_SIGNATURE_MISMATCH
                && problem.message().contains("DATEADD")
        ));
    }

    @Test
    void validate_reportsUnsupportedSqlServerDmlExtensions() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var insertStatement = insert("users")
            .ignore()
            .columns(id("id"), id("name"))
            .values(rows(row(lit(1L), lit("alice"))))
            .onConflictDoNothing(id("id"))
            .returning(col("id").toSelectItem())
            .build();
        var updateStatement = update("users")
            .set(Identifier.of("name"), lit("alice"))
            .join(inner(tbl("users").as("u2")).on(col("u2", "id").eq(col("users", "id"))))
            .from(tbl("users").as("u3"))
            .returning(col("id").toSelectItem())
            .optimizerHint("INDEX(users idx_users_name)")
            .build();
        var deleteStatement = delete("users")
            .using(tbl("users").as("u"))
            .join(inner(tbl("users").as("u2")).on(col("u2", "id").eq(col("users", "id"))))
            .returning(col("id").toSelectItem())
            .optimizerHint("INDEX(users idx_users_name)")
            .build();

        var insertResult = validator.validate(insertStatement);
        var updateResult = validator.validate(updateStatement);
        var deleteResult = validator.validate(deleteStatement);

        assertTrue(insertResult.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "insert.mode".equals(problem.clausePath())
        ));
        assertTrue(insertResult.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "insert.on_conflict".equals(problem.clausePath())
        ));
        assertTrue(insertResult.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "insert.returning".equals(problem.clausePath())
        ));

        assertTrue(updateResult.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "update.join".equals(problem.clausePath())
        ));
        assertTrue(updateResult.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "update.from".equals(problem.clausePath())
        ));
        assertTrue(updateResult.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "update.returning".equals(problem.clausePath())
        ));
        assertTrue(updateResult.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "update.hint".equals(problem.clausePath())
        ));

        assertTrue(deleteResult.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "delete.using".equals(problem.clausePath())
        ));
        assertTrue(deleteResult.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "delete.join".equals(problem.clausePath())
        ));
        assertTrue(deleteResult.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "delete.returning".equals(problem.clausePath())
        ));
        assertTrue(deleteResult.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "delete.hint".equals(problem.clausePath())
        ));
    }

    @Test
    void dialect_exposesSqlServerRules() {
        var dialect = SqlServerValidationDialect.of();

        assertEquals("sqlserver", dialect.name());
        assertEquals(4, dialect.additionalRules().size());
        assertTrue(dialect.functionCatalog().resolve("len").isPresent());
        assertTrue(dialect.functionCatalog().resolve("getdate").isPresent());
    }

    @Test
    void functionCatalogFallsBackAndHandlesNullFunctionNames() {
        var catalog = SqlServerValidationDialect.of().functionCatalog();

        assertTrue(catalog.resolve("count").isPresent());
        assertTrue(catalog.resolve("STRING_AGG").isPresent());
        assertTrue(catalog.resolve(null).isEmpty());
    }

    private static boolean hasDialectProblem(io.sqm.validate.api.ValidationResult result, ValidationProblem.Code code) {
        return result.problems().stream().anyMatch(problem -> problem.code() == code);
    }
}
