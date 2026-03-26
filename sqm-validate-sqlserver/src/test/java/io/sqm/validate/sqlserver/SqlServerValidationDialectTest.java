package io.sqm.validate.sqlserver;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.core.Identifier;
import io.sqm.core.LimitOffset;
import io.sqm.core.SelectModifier;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.SchemaStatementValidator;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

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

    private static boolean hasDialectProblem(io.sqm.validate.api.ValidationResult result, ValidationProblem.Code code) {
        return result.problems().stream().anyMatch(problem -> problem.code() == code);
    }

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
    void validate_acceptsTopPercent() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var query = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .top(topPercent(lit(10L)))
            .build();

        var result = validator.validate(query);

        assertTrue(result.ok(), result.problems().toString());
    }

    @Test
    void validate_acceptsTopWithTiesWithOrderBy() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var query = select(col("u", "id"))
            .from(tbl("users").as("u").withNoLock())
            .orderBy(order(col("u", "id")))
            .top(topWithTies(lit(10L)))
            .build();

        var result = validator.validate(query);

        assertTrue(result.ok(), result.problems().toString());
    }

    @Test
    void validate_reportsTopWithTiesWithoutOrderBy() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var query = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .top(topWithTies(lit(10L)))
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "select.top".equals(problem.clausePath())
                && problem.message().contains("WITH TIES")
                && problem.message().contains("ORDER BY")
        ));
    }

    @Test
    void validate_reportsUnsupportedSqlServerSelectExtensions() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var query = select(col("u", "id"))
            .from(tbl("users").as("u").withHints(java.util.List.of(
                io.sqm.core.TableHint.of("USE_INDEX", Identifier.of("idx_users_name"))
            )))
            .selectModifier(SelectModifier.CALC_FOUND_ROWS)
            .hint("INDEX", "users", "idx_users_name")
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
    void validate_reportsConflictingSqlServerTableHints() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var query = select(col("u", "id"))
            .from(tbl("users").as("u").withNoLock().withUpdLock())
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "from.table".equals(problem.clausePath())
                && problem.message().contains("NOLOCK")
        ));
    }

    @Test
    void validate_reportsDuplicateSqlServerTableHints() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var query = select(col("u", "id"))
            .from(tbl("users").as("u").withHoldLock().withHoldLock())
            .build();

        var result = validator.validate(query);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "from.table".equals(problem.clausePath())
                && problem.message().contains("Duplicate")
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
        var insertStatement = insert(tbl("users").withHoldLock())
            .columns(id("id"), id("name"))
            .result(inserted("id").as("user_id"))
            .values(rows(row(lit(1L), lit("alice"))))
            .build();
        var updateStatement = update(tbl("users").withUpdLock())
            .set(Identifier.of("missing_col"), lit("alice"))
            .result(deleted("name"), inserted("name"))
            .build();
        var deleteStatement = delete(tbl("users").withNoLock())
            .result(deleted("id"))
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
    void validate_acceptsSqlServerDmlWithoutResultClause() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var insertStatement = insert(tbl("users").withHoldLock())
            .columns(id("id"), id("name"))
            .values(rows(row(lit(1L), lit("alice"))))
            .build();
        var updateStatement = update(tbl("users").withUpdLock())
            .set(Identifier.of("name"), lit("alice"))
            .where(col("id").eq(lit(1L)))
            .build();
        var deleteStatement = delete(tbl("users").withNoLock())
            .where(col("id").eq(lit(1L)))
            .build();

        var insertResult = validator.validate(insertStatement);
        var updateResult = validator.validate(updateStatement);
        var deleteResult = validator.validate(deleteStatement);

        assertTrue(insertResult.ok(), insertResult.problems().toString());
        assertTrue(updateResult.ok(), updateResult.problems().toString());
        assertTrue(deleteResult.ok(), deleteResult.problems().toString());
    }

    @Test
    void validate_reportsOutputIntoTargetHintsAsUnsupported() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var updateStatement = update("users")
            .set(Identifier.of("name"), lit("alice"))
            .result(resultInto(tbl("audit").withNoLock(), "user_id"), inserted("id"))
            .build();

        var result = validator.validate(updateStatement);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "update.result".equals(problem.clausePath())
                && problem.message().contains("OUTPUT INTO")
        ));
    }

    @Test
    void validate_acceptsTableVariableOutputIntoTarget() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var updateStatement = update("users")
            .set(Identifier.of("name"), lit("alice"))
            .result(resultInto(tableVar("@audit"), "user_id"), inserted("id"))
            .build();

        var result = validator.validate(updateStatement);

        assertFalse(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "update.result".equals(problem.clausePath())
        ));
    }

    @Test
    void validate_acceptsBaseTableOutputIntoTargetWithoutHints() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var updateStatement = update("users")
            .set(Identifier.of("name"), lit("alice"))
            .result(resultInto(tbl("users"), "id"), inserted("id"))
            .build();

        var result = validator.validate(updateStatement);

        assertFalse(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "update.result".equals(problem.clausePath())
        ));
    }

    @Test
    void validate_reportsInvalidSqlServerOutputRowSources() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var insertStatement = insert("users")
            .columns(id("id"), id("name"))
            .result(deletedAll(), deleted("id"))
            .values(rows(row(lit(1L), lit("alice"))))
            .build();
        var deleteStatement = delete("users")
            .result(insertedAll(), inserted("id"))
            .where(col("id").eq(lit(1L)))
            .build();

        var insertResult = validator.validate(insertStatement);
        var deleteResult = validator.validate(deleteStatement);

        assertTrue(insertResult.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "insert.result".equals(problem.clausePath())
                && problem.message().contains("deleted")
        ));
        assertTrue(deleteResult.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "delete.result".equals(problem.clausePath())
                && problem.message().contains("inserted")
        ));
    }

    @Test
    void validate_acceptsFirstSliceSqlServerMerge() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var mergeStatement = merge(tbl("users").withHoldLock())
            .source(tbl("users").as("s").withUpdLock())
            .on(col("users", "id").eq(col("s", "id")))
            .top(topPercent(lit(10L)))
            .whenMatchedUpdate(col("s", "updated_at").isNotNull(), java.util.List.of(set("users", "name", col("s", "name"))))
            .whenNotMatchedBySourceDelete(col("users", "updated_at").isNotNull())
            .whenNotMatchedInsert(java.util.List.of(id("id"), id("name")), row(col("s", "id"), col("s", "name")))
            .result(deleted("id"), inserted("id"))
            .build();

        var result = validator.validate(mergeStatement);

        assertTrue(result.ok(), result.problems().toString());
    }

    @Test
    void validate_reportsSqlServerMergeClauseConflictsAndOutputIntoTargetHints() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var mergeStatement = merge("users")
            .source(tbl("users").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedUpdate(java.util.List.of(set("users", "name", col("s", "name"))))
            .whenMatchedDelete()
            .result(resultInto(tbl("audit").withNoLock(), "user_id"), inserted("id"))
            .build();

        var result = validator.validate(mergeStatement);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "merge.clause".equals(problem.clausePath())
                && problem.message().contains("first WHEN MATCHED")
        ));
        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "merge.result".equals(problem.clausePath())
                && problem.message().contains("OUTPUT INTO")
        ));
    }

    @Test
    void validate_reportsNonTableOutputIntoTargetAsUnsupported() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var updateStatement = update("users")
            .set(Identifier.of("name"), lit("alice"))
            .result(resultInto(tbl(select(lit(1L)).build()).as("audit_rows"), "user_id"), inserted("id"))
            .build();

        var result = validator.validate(updateStatement);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "update.result".equals(problem.clausePath())
                && problem.message().contains("base tables and table variables")
        ));
    }

    @Test
    void validate_reportsSqlServerMergeHintAndClauseConflicts() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var mergeStatement = merge(tbl("users").withNoLock().withUpdLock())
            .source(tbl("users").as("s").withHoldLock().withHoldLock())
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedDelete(col("s", "updated_at").isNotNull())
            .whenMatchedDelete()
            .whenNotMatchedInsert(java.util.List.of(id("id")), row(col("s", "id")))
            .whenNotMatchedInsert(java.util.List.of(id("id")), row(col("s", "id")))
            .build();

        var result = validator.validate(mergeStatement);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "merge.target".equals(problem.clausePath())
        ));
        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "merge.source".equals(problem.clausePath())
        ));
        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "merge.clause".equals(problem.clausePath())
                && problem.message().contains("DELETE")
        ));
        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "merge.clause".equals(problem.clausePath())
                && problem.message().contains("INSERT")
        ));
    }

    @Test
    void validate_reportsSqlServerMergeWithTooManyMatchedClauses() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var mergeStatement = merge("users")
            .source(tbl("users").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedUpdate(col("s", "updated_at").isNotNull(), java.util.List.of(set("users", "name", col("s", "name"))))
            .whenMatchedDelete()
            .whenMatchedDelete()
            .build();

        var result = validator.validate(mergeStatement);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "merge.clause".equals(problem.clausePath())
                && problem.message().contains("at most two WHEN MATCHED")
        ));
    }

    @Test
    void validate_reportsSqlServerMergeBySourceClauseConflicts() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var mergeStatement = merge("users")
            .source(tbl("users").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenNotMatchedBySourceDelete()
            .whenNotMatchedBySourceUpdate(java.util.List.of(set("users", "name", col("s", "name"))))
            .build();

        var result = validator.validate(mergeStatement);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "merge.clause".equals(problem.clausePath())
                && problem.message().contains("first WHEN NOT MATCHED BY SOURCE")
        ));
    }

    @Test
    void validate_reportsSqlServerMergeDoNothingAsUnsupported() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var mergeStatement = merge("users")
            .source(tbl("users").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedDoNothing()
            .build();

        var result = validator.validate(mergeStatement);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "merge.clause".equals(problem.clausePath())
                && problem.message().contains("DO NOTHING")
        ));
    }

    @Test
    void validate_reportsSqlServerMergeTopWithTiesAsInvalid() {
        var validator = SchemaStatementValidator.of(SCHEMA, SqlServerValidationDialect.of());
        var mergeStatement = merge("users")
            .source(tbl("users").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .top(topWithTies(lit(5L)))
            .whenMatchedDelete()
            .build();

        var result = validator.validate(mergeStatement);

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "merge.top".equals(problem.clausePath())
                && problem.message().contains("WITH TIES")
        ));
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
            .result(col("id").toSelectItem())
            .build();
        var updateStatement = update("users")
            .set(Identifier.of("name"), lit("alice"))
            .join(inner(tbl("users").as("u2")).on(col("u2", "id").eq(col("users", "id"))))
            .from(tbl("users").as("u3"))
            .result(col("id").toSelectItem())
            .hint("INDEX", "users", "idx_users_name")
            .build();
        var deleteStatement = delete("users")
            .using(tbl("users").as("u"))
            .join(inner(tbl("users").as("u2")).on(col("u2", "id").eq(col("users", "id"))))
            .result(col("id").toSelectItem())
            .hint("INDEX", "users", "idx_users_name")
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
                && "delete.hint".equals(problem.clausePath())
        ));
    }

    @Test
    void dialect_exposesSqlServerRules() {
        var dialect = SqlServerValidationDialect.of();

        assertEquals("sqlserver", dialect.name());
        assertEquals(5, dialect.additionalRules().size());
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
}
