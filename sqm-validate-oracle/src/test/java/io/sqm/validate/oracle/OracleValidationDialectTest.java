package io.sqm.validate.oracle;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.core.MergeClause;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.SchemaStatementValidator;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.delete;
import static io.sqm.dsl.Dsl.func;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.insert;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.merge;
import static io.sqm.dsl.Dsl.param;
import static io.sqm.dsl.Dsl.resultVariableTarget;
import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.set;
import static io.sqm.dsl.Dsl.tbl;
import static io.sqm.dsl.Dsl.update;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OracleValidationDialectTest {
    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "users",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("name", CatalogType.STRING)
        ),
        CatalogTable.of("public", "src_users",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("name", CatalogType.STRING)
        )
    );

    @Test
    void exposesOracleDialectIdentityAndCapabilities() {
        var dialect = OracleValidationDialect.of();

        assertEquals("oracle", dialect.name());
        assertTrue(dialect.capabilities().supports(io.sqm.core.dialect.SqlFeature.MERGE_STATEMENT));
        assertTrue(dialect.functionCatalog().resolve("nvl").isPresent());
        assertThrows(NullPointerException.class, () -> OracleValidationDialect.of(null));
    }

    @Test
    void validatesBaselineOracleDmlAndMerge() {
        var validator = SchemaStatementValidator.of(SCHEMA, OracleValidationDialect.of());
        var insert = insert("users").columns(id("id"), id("name")).values(row(lit(1L), lit("alice"))).build();
        var update = update("users").set("name", lit("alice")).where(col("id").eq(lit(1L))).build();
        var delete = delete("users").where(col("id").eq(lit(1L))).build();
        var merge = merge("users")
            .source(tbl("src_users").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedUpdate(java.util.List.of(set("name", col("s", "name"))))
            .whenNotMatchedInsert(java.util.List.of(id("id"), id("name")), row(col("s", "id"), col("s", "name")))
            .build();

        assertFalse(hasDialectProblem(validator.validate(insert), "insert.result"));
        assertFalse(hasDialectProblem(validator.validate(update), "update.result"));
        assertFalse(hasDialectProblem(validator.validate(delete), "delete.result"));
        assertFalse(hasDialectProblem(validator.validate(merge), "merge.result"));
    }

    @Test
    void rejectsOracleDmlResultClauses() {
        var validator = SchemaStatementValidator.of(SCHEMA, OracleValidationDialect.of());

        assertTrue(hasDialectProblem(
            validator.validate(insert("users").columns(id("id")).values(row(lit(1L))).result(col("id")).build()),
            "insert.result"
        ));
        assertTrue(hasDialectProblem(
            validator.validate(update("users").set("name", lit("alice")).result(col("id")).build()),
            "update.result"
        ));
        assertTrue(hasDialectProblem(
            validator.validate(delete("users").result(col("id")).build()),
            "delete.result"
        ));
    }

    @Test
    void validatesOracleReturningIntoVariableTargets() {
        var validator = SchemaStatementValidator.of(SCHEMA, OracleValidationDialect.of());
        var valid = insert("users")
            .columns(id("id"), id("name"))
            .values(row(lit(1L), lit("alice")))
            .result(resultVariableTarget(param("id"), param("name")), col("id"), col("name"))
            .build();
        var mismatch = update("users")
            .set("name", lit("alice"))
            .result(resultVariableTarget(param("id")), col("id"), col("name"))
            .build();

        assertFalse(hasDialectProblem(validator.validate(valid), "insert.result"));
        assertTrue(validator.validate(mismatch).problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "update.result".equals(problem.clausePath())
        ));
    }

    @Test
    void validatesOracleFunctionCatalogSignatures() {
        var validator = SchemaStatementValidator.of(SCHEMA, OracleValidationDialect.of());
        var query = io.sqm.dsl.Dsl.select(
            func("nvl", col("u", "name"), lit("unknown")),
            func("to_char", col("u", "id")),
            func("sysdate")
        ).from(tbl("users").as("u")).build();

        assertTrue(validator.validate(query).ok());
    }

    @Test
    void rejectsOracleFunctionSignatureMismatches() {
        var validator = SchemaStatementValidator.of(SCHEMA, OracleValidationDialect.of());
        var arityMismatch = io.sqm.dsl.Dsl.select(func("nvl", col("u", "name")))
            .from(tbl("users").as("u"))
            .build();
        var typeMismatch = io.sqm.dsl.Dsl.select(func("substr", col("u", "id"), lit(1L)))
            .from(tbl("users").as("u"))
            .build();

        assertTrue(hasFunctionProblem(validator.validate(arityMismatch)));
        assertTrue(hasFunctionProblem(validator.validate(typeMismatch)));
    }

    @Test
    void rejectsNonOracleInsertModesAndConflictClauses() {
        var validator = SchemaStatementValidator.of(SCHEMA, OracleValidationDialect.of());

        assertTrue(hasDialectProblem(
            validator.validate(insert("users").ignore().columns(id("id")).values(row(lit(1L))).build()),
            "insert.mode"
        ));
        assertTrue(hasDialectProblem(
            validator.validate(insert("users").replace().columns(id("id")).values(row(lit(1L))).build()),
            "insert.mode"
        ));
        assertTrue(hasDialectProblem(
            validator.validate(insert("users").columns(id("id")).values(row(lit(1L))).onConflictDoNothing().build()),
            "insert.conflict"
        ));
    }

    @Test
    void rejectsNonOracleUpdateFromAndJoinClauses() {
        var validator = SchemaStatementValidator.of(SCHEMA, OracleValidationDialect.of());

        assertTrue(hasDialectProblem(
            validator.validate(update("users").set("name", lit("alice")).from(tbl("src_users")).build()),
            "update.from"
        ));
        assertTrue(hasDialectProblem(
            validator.validate(update("users").set("name", lit("alice")).join(io.sqm.core.CrossJoin.of(tbl("src_users"))).build()),
            "update.join"
        ));
    }

    @Test
    void rejectsNonOracleDeleteUsingAndJoinClauses() {
        var validator = SchemaStatementValidator.of(SCHEMA, OracleValidationDialect.of());

        assertTrue(hasDialectProblem(
            validator.validate(delete("users").using(tbl("src_users")).build()),
            "delete.using"
        ));
        assertTrue(hasDialectProblem(
            validator.validate(delete("users").join(io.sqm.core.CrossJoin.of(tbl("src_users"))).build()),
            "delete.using"
        ));
    }

    @Test
    void rejectsNonOracleMergeShapes() {
        var validator = SchemaStatementValidator.of(SCHEMA, OracleValidationDialect.of());
        var top = merge("users")
            .source(tbl("src_users").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .top(1)
            .whenMatchedUpdate(java.util.List.of(set("name", col("s", "name"))))
            .build();
        var result = merge("users")
            .source(tbl("src_users").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedUpdate(java.util.List.of(set("name", col("s", "name"))))
            .result(col("id"))
            .build();
        var bySource = merge("users")
            .source(tbl("src_users").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .clause(MergeClause.of(
                MergeClause.MatchType.NOT_MATCHED_BY_SOURCE,
                null,
                io.sqm.core.MergeUpdateAction.of(java.util.List.of(set("name", col("s", "name"))))
            ))
            .build();
        var doNothing = merge("users")
            .source(tbl("src_users").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedDoNothing()
            .build();

        assertTrue(hasDialectProblem(validator.validate(top), "merge.top"));
        assertTrue(hasDialectProblem(validator.validate(result), "merge.result"));
        assertTrue(hasDialectProblem(validator.validate(bySource), "merge.clause"));
        assertTrue(hasDialectProblem(validator.validate(doNothing), "merge.action"));
    }

    private static boolean hasDialectProblem(io.sqm.validate.api.ValidationResult result, String clausePath) {
        return result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && clausePath.equals(problem.clausePath())
        );
    }

    private static boolean hasFunctionProblem(io.sqm.validate.api.ValidationResult result) {
        return result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.FUNCTION_SIGNATURE_MISMATCH
                && "function.call".equals(problem.clausePath())
        );
    }
}
