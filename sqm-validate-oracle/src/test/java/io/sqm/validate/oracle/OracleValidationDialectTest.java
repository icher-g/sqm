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
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.insert;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.merge;
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
}
