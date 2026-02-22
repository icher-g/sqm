package io.sqm.validate;

import io.sqm.catalog.access.DefaultCatalogAccessPolicy;
import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.core.*;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.SchemaQueryValidator;
import io.sqm.validate.schema.SchemaValidationLimits;
import io.sqm.validate.schema.SchemaValidationSettings;
import io.sqm.validate.schema.dialect.SchemaValidationDialect;
import io.sqm.validate.schema.function.FunctionArgKind;
import io.sqm.validate.schema.function.FunctionCatalog;
import io.sqm.validate.schema.function.FunctionSignature;
import io.sqm.validate.schema.rule.SchemaValidationRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class SchemaQueryValidatorTest {
    // Touch file to force test recompilation after Dsl.select(...) return-type refactor.

    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "users",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("name", CatalogType.STRING),
            CatalogColumn.of("age", CatalogType.INTEGER),
            CatalogColumn.of("status", CatalogType.STRING)
        ),
        CatalogTable.of("public", "orders",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("user_id", CatalogType.LONG),
            CatalogColumn.of("status", CatalogType.STRING)
        ),
        CatalogTable.of("public", "accounts",
            CatalogColumn.of("id", CatalogType.STRING),
            CatalogColumn.of("status", CatalogType.STRING)
        )
    );

    private final SchemaQueryValidator validator = SchemaQueryValidator.of(SCHEMA);

    @Test
    void validate_okForExistingTablesAndColumns() {
        Query query = select(
            col("u", "name"),
            col("o", "status")
        )
            .from(tbl("users").as("u"))
            .join(inner(tbl("orders").as("o")).on(col("o", "user_id").eq(col("u", "id"))))
            .where(col("u", "status").eq(lit("active")))
            .build();

        var result = validator.validate(query);
        assertTrue(result.ok());
    }

    @Test
    void validate_reportsMissingTable() {
        Query query = select(star()).from(tbl("missing_users")).build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream().anyMatch(p -> p.code() == ValidationProblem.Code.TABLE_NOT_FOUND));
    }

    @Test
    void validate_reportsDeniedTableByPolicy() {
        var settings = SchemaValidationSettings.builder()
            .accessPolicy(DefaultCatalogAccessPolicy.builder().denyTable("orders").build())
            .build();
        var policyValidator = SchemaQueryValidator.of(SCHEMA, settings);

        Query query = select(star()).from(tbl("orders").as("o")).build();
        var result = policyValidator.validate(query);

        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.POLICY_TABLE_DENIED));
    }

    @Test
    void validate_reportsMissingColumn() {
        Query query = select(col("u", "unknown_col")).from(tbl("users").as("u")).build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream().anyMatch(p -> p.code() == ValidationProblem.Code.COLUMN_NOT_FOUND));
    }

    @Test
    void validate_appliesPrincipalSpecificTablePolicy() {
        var policy = DefaultCatalogAccessPolicy.builder()
            .denyTableForPrincipal("alice", "orders")
            .build();
        var settings = SchemaValidationSettings.builder()
            .principal("alice")
            .accessPolicy(policy)
            .build();
        var policyValidator = SchemaQueryValidator.of(SCHEMA, settings);

        Query query = select(star()).from(tbl("orders").as("o")).build();
        var result = policyValidator.validate(query);

        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.POLICY_TABLE_DENIED));
    }

    @Test
    void validate_reportsDeniedColumnByPolicy() {
        var settings = SchemaValidationSettings.builder()
            .accessPolicy(DefaultCatalogAccessPolicy.builder().denyColumn("u.status").build())
            .build();
        var policyValidator = SchemaQueryValidator.of(SCHEMA, settings);

        Query query = select(col("u", "status")).from(tbl("users").as("u")).build();
        var result = policyValidator.validate(query);

        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.POLICY_COLUMN_DENIED));
    }

    @Test
    void validate_reportsUnknownAlias() {
        Query query = select(col("x", "id")).from(tbl("users").as("u")).build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream().anyMatch(p -> p.code() == ValidationProblem.Code.UNKNOWN_TABLE_ALIAS));
    }

    @Test
    void validate_reportsAmbiguousUnqualifiedColumn() {
        Query query = select(col("id"))
            .from(tbl("users").as("u"))
            .join(inner(tbl("orders").as("o")).on(col("o", "user_id").eq(col("u", "id"))))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream().anyMatch(p -> p.code() == ValidationProblem.Code.COLUMN_AMBIGUOUS));
    }

    @Test
    void validate_supportsCorrelatedSubquery() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .where(exists(
                select(star())
                    .from(tbl("orders").as("o"))
                    .where(col("o", "user_id").eq(col("u", "id")))
                    .build()
            ))
            .build();

        var result = validator.validate(query);
        assertTrue(result.ok());
    }

    @Test
    void validate_reportsInvalidUsingColumn() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .join(inner(tbl("orders").as("o")).using("missing_col"))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream().anyMatch(p -> p.code() == ValidationProblem.Code.JOIN_USING_INVALID_COLUMN));
    }

    @Test
    void validate_reportsOnJoinReferenceToLaterAlias() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .join(inner(tbl("orders").as("o")).on(col("o", "user_id").eq(col("a", "id"))))
            .join(inner(tbl("accounts").as("a")).on(col("a", "id").eq(col("u", "id"))))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.JOIN_ON_INVALID_REFERENCE));
    }

    @Test
    void validate_acceptsOnJoinReferenceToLeftAndCurrentRightAliasesOnly() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .join(inner(tbl("orders").as("o")).on(col("o", "user_id").eq(col("u", "id"))))
            .join(inner(tbl("accounts").as("a")).on(col("a", "id").eq(col("u", "id"))))
            .build();

        var result = validator.validate(query);
        assertTrue(result.problems().stream()
            .noneMatch(p -> p.code() == ValidationProblem.Code.JOIN_ON_INVALID_REFERENCE));
    }

    @Test
    void validate_reportsMissingOnPredicateForRegularJoin() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .join(inner(tbl("orders").as("o")))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.JOIN_ON_MISSING_PREDICATE));
    }

    @Test
    void validate_reportsJoinOnInvalidBooleanExpression() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .join(inner(tbl("orders").as("o")).on(unary(lit(1))))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.JOIN_ON_INVALID_BOOLEAN_EXPRESSION
                && "join.on".equals(p.clausePath())));
    }

    @Test
    void validate_acceptsJoinOnBooleanUnaryExpression() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .join(inner(tbl("orders").as("o")).on(unary(lit(true))))
            .build();

        var result = validator.validate(query);
        assertTrue(result.problems().stream()
            .noneMatch(p -> p.code() == ValidationProblem.Code.JOIN_ON_INVALID_BOOLEAN_EXPRESSION));
    }

    @Test
    void validate_reportsTypeMismatchForUsingColumn() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .join(inner(tbl("accounts").as("a")).using("id"))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream().anyMatch(p -> p.code() == ValidationProblem.Code.TYPE_MISMATCH));
    }

    @Test
    void validate_reportsFunctionNotAllowedByPolicy() {
        var settings = SchemaValidationSettings.builder()
            .accessPolicy(DefaultCatalogAccessPolicy.builder().allowFunction("length").build())
            .build();
        var policyValidator = SchemaQueryValidator.of(SCHEMA, settings);

        Query query = select(func("lower", arg(col("u", "name"))))
            .from(tbl("users").as("u"))
            .build();
        var result = policyValidator.validate(query);

        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.POLICY_FUNCTION_NOT_ALLOWED));
    }

    @Test
    void validate_acceptsAllowedFunctionByPolicy() {
        var settings = SchemaValidationSettings.builder()
            .accessPolicy(DefaultCatalogAccessPolicy.builder().allowFunction("length").build())
            .build();
        var policyValidator = SchemaQueryValidator.of(SCHEMA, settings);

        Query query = select(func("length", arg(col("u", "name"))))
            .from(tbl("users").as("u"))
            .build();
        var result = policyValidator.validate(query);

        assertTrue(result.problems().stream()
            .noneMatch(p -> p.code() == ValidationProblem.Code.POLICY_FUNCTION_NOT_ALLOWED));
    }

    @Test
    void validate_reportsMaxJoinLimitExceeded() {
        var settings = SchemaValidationSettings.builder()
            .limits(SchemaValidationLimits.builder().maxJoinCount(1).build())
            .build();
        var policyValidator = SchemaQueryValidator.of(SCHEMA, settings);

        Query query = select(star())
            .from(tbl("users").as("u"))
            .join(inner(tbl("orders").as("o")).on(col("o", "user_id").eq(col("u", "id"))))
            .join(inner(tbl("accounts").as("a")).on(col("a", "id").eq(col("u", "id"))))
            .build();

        var result = policyValidator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.POLICY_MAX_JOINS_EXCEEDED));
    }

    @Test
    void validate_acceptsBoundaryMaxJoinLimit() {
        var settings = SchemaValidationSettings.builder()
            .limits(SchemaValidationLimits.builder().maxJoinCount(1).build())
            .build();
        var policyValidator = SchemaQueryValidator.of(SCHEMA, settings);

        Query query = select(star())
            .from(tbl("users").as("u"))
            .join(inner(tbl("orders").as("o")).on(col("o", "user_id").eq(col("u", "id"))))
            .build();

        var result = policyValidator.validate(query);
        assertTrue(result.problems().stream()
            .noneMatch(p -> p.code() == ValidationProblem.Code.POLICY_MAX_JOINS_EXCEEDED));
    }

    @Test
    void validate_reportsMaxSelectColumnsExceeded() {
        var settings = SchemaValidationSettings.builder()
            .limits(SchemaValidationLimits.builder().maxSelectColumns(2).build())
            .build();
        var policyValidator = SchemaQueryValidator.of(SCHEMA, settings);

        Query query = select(col("u", "id"), col("u", "name"), col("u", "status"))
            .from(tbl("users").as("u"))
            .build();

        var result = policyValidator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.POLICY_MAX_SELECT_COLUMNS_EXCEEDED));
    }

    @Test
    void validate_acceptsBoundaryMaxSelectColumnsLimit() {
        var settings = SchemaValidationSettings.builder()
            .limits(SchemaValidationLimits.builder().maxSelectColumns(2).build())
            .build();
        var policyValidator = SchemaQueryValidator.of(SCHEMA, settings);

        Query query = select(col("u", "id"), col("u", "name"))
            .from(tbl("users").as("u"))
            .build();

        var result = policyValidator.validate(query);
        assertTrue(result.problems().stream()
            .noneMatch(p -> p.code() == ValidationProblem.Code.POLICY_MAX_SELECT_COLUMNS_EXCEEDED));
    }

    @Test
    void validate_reportsTypeMismatchWhenTypesAreKnown() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .where(col("u", "age").eq(lit("not_numeric")))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream().anyMatch(p -> p.code() == ValidationProblem.Code.TYPE_MISMATCH));
    }

    @Test
    void validate_reportsTypeMismatchForBetween() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .where(col("u", "age").between(lit("low"), lit("high")))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream().anyMatch(p -> p.code() == ValidationProblem.Code.TYPE_MISMATCH));
    }

    @Test
    void validate_reportsTypeMismatchForLike() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .where(col("u", "age").like(lit("%1%")))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream().anyMatch(p -> p.code() == ValidationProblem.Code.TYPE_MISMATCH));
    }

    @Test
    void validate_reportsTypeMismatchForIn() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .where(col("u", "age").in("a", "b"))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream().anyMatch(p -> p.code() == ValidationProblem.Code.TYPE_MISMATCH));
    }

    @Test
    void validate_reportsInRowShapeMismatchForScalarLeftAndTupleRight() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .where(col("u", "id").in(rows(row(lit(1), lit("active")))))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream().anyMatch(p -> p.code() == ValidationProblem.Code.IN_ROW_SHAPE_MISMATCH));
    }

    @Test
    void validate_reportsInRowShapeMismatchForTupleLeftAndTupleRight() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .where(row(col("u", "id"), col("u", "status")).in(
                row(lit(1))
            ))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream().anyMatch(p -> p.code() == ValidationProblem.Code.IN_ROW_SHAPE_MISMATCH));
    }

    @Test
    void validate_reportsInRowShapeMismatchForTupleLeftAndSubqueryArity() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .where(row(col("u", "id"), col("u", "status")).in(
                Expression.subquery(select(col("id")).from(tbl("orders").as("o")).build())
            ))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream().anyMatch(p -> p.code() == ValidationProblem.Code.IN_ROW_SHAPE_MISMATCH));
    }

    @Test
    void validate_acceptsInTupleShapeAndComparableTypes() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .where(row(col("u", "id"), col("u", "status")).in(
                rows(
                    row(lit(1L), lit("active")),
                    row(lit(2L), lit("inactive"))
                )
            ))
            .build();

        var result = validator.validate(query);
        assertTrue(result.problems().stream()
            .noneMatch(p -> p.code() == ValidationProblem.Code.IN_ROW_SHAPE_MISMATCH));
    }

    @Test
    void validate_reportsInTupleTypeMismatchAgainstSubqueryColumn() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .where(row(col("u", "id"), col("u", "status")).in(
                Expression.subquery(
                    select(col("o", "status"), col("o", "id"))
                        .from(tbl("orders").as("o"))
                        .build()
                )
            ))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream().anyMatch(p -> p.code() == ValidationProblem.Code.TYPE_MISMATCH));
    }

    @Test
    void validate_acceptsInTupleComparableTypesAgainstSubquery() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .where(row(col("u", "id"), col("u", "status")).in(
                Expression.subquery(
                    select(col("o", "id"), col("o", "status"))
                        .from(tbl("orders").as("o"))
                        .build()
                )
            ))
            .build();

        var result = validator.validate(query);
        assertTrue(result.problems().stream()
            .noneMatch(p -> p.code() == ValidationProblem.Code.TYPE_MISMATCH
                || p.code() == ValidationProblem.Code.IN_ROW_SHAPE_MISMATCH));
    }

    @Test
    void validate_reportsTypeMismatchForAnyAll() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .where(col("u", "age").all(ComparisonOperator.GT,
                select(col("status"))
                    .from(tbl("orders").as("o"))
                    .build()
            ))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream().anyMatch(p -> p.code() == ValidationProblem.Code.TYPE_MISMATCH));
    }

    @Test
    void validate_reportsInvalidLimitTypeForScalarSubqueryExpression() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .limit(Expression.subquery(select(col("status")).from(tbl("orders").as("o")).build()))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.LIMIT_OFFSET_INVALID));
    }

    @Test
    void validate_acceptsNumericLimitForScalarSubqueryExpression() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .limit(Expression.subquery(select(col("id")).from(tbl("orders").as("o")).build()))
            .build();

        var result = validator.validate(query);
        assertTrue(result.problems().stream()
            .noneMatch(p -> p.code() == ValidationProblem.Code.LIMIT_OFFSET_INVALID));
    }

    @Test
    void validate_reportsSubqueryShapeMismatchForLimitExpression() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .limit(Expression.subquery(select(col("id"), col("status")).from(tbl("orders").as("o")).build()))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.SUBQUERY_SHAPE_MISMATCH));
    }

    @Test
    void validate_reportsSubqueryShapeMismatchForAnyAll() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .where(col("u", "age").all(
                ComparisonOperator.GT,
                select(col("id"), col("status")).from(tbl("orders").as("o")).build()
            ))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream().anyMatch(p -> p.code() == ValidationProblem.Code.SUBQUERY_SHAPE_MISMATCH));
    }

    @Test
    void validate_reportsSubqueryShapeMismatchForIn() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .where(col("u", "age").in(Expression.subquery(
                select(col("id"), col("status")).from(tbl("orders").as("o")).build())))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream().anyMatch(p -> p.code() == ValidationProblem.Code.SUBQUERY_SHAPE_MISMATCH));
    }

    @Test
    void validate_reportsSubqueryShapeMismatchForBetweenBound() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .where(col("u", "age").between(
                Expression.subquery(select(col("id"), col("status")).from(tbl("orders").as("o")).build()),
                lit(10)
            ))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream().anyMatch(p -> p.code() == ValidationProblem.Code.SUBQUERY_SHAPE_MISMATCH));
    }

    @Test
    void validate_reportsSubqueryShapeMismatchForIsDistinctFromOperand() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .where(col("u", "id").isDistinctFrom(Expression.subquery(
                select(col("id"), col("status")).from(tbl("orders").as("o")).build()
            )))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream().anyMatch(p -> p.code() == ValidationProblem.Code.SUBQUERY_SHAPE_MISMATCH));
    }

    @Test
    void validate_reportsSubqueryShapeMismatchForLikePattern() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .where(col("u", "name").like(Expression.subquery(
                select(col("id"), col("status")).from(tbl("orders").as("o")).build()
            )))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream().anyMatch(p -> p.code() == ValidationProblem.Code.SUBQUERY_SHAPE_MISMATCH));
    }

    @Test
    void validate_reportsSubqueryShapeMismatchForUnaryPredicateExpression() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .where(unary(Expression.subquery(
                select(col("id"), col("status")).from(tbl("orders").as("o")).build()
            )))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream().anyMatch(p -> p.code() == ValidationProblem.Code.SUBQUERY_SHAPE_MISMATCH));
    }

    @Test
    void validate_reportsTypeMismatchForIsDistinctFrom() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .where(col("u", "age").isDistinctFrom(lit("x")))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream().anyMatch(p -> p.code() == ValidationProblem.Code.TYPE_MISMATCH));
    }

    @Test
    void validate_supportsCteNameWithExplicitAliases() {
        Query query = with(
            cte("active_users",
                select(col("id")).from(tbl("users")).where(col("status").eq(lit("active"))).build()
            ).columnAliases("id")
        ).body(
            select(col("a", "id")).from(tbl("active_users").as("a")).build()
        );

        var result = validator.validate(query);
        assertTrue(result.ok());
    }

    @Test
    void validate_reportsCteColumnAliasCountMismatch() {
        Query query = with(
            cte("user_stats",
                select(col("id"), col("status")).from(tbl("users")).build()
            ).columnAliases("id")
        ).body(
            select(col("s", "id")).from(tbl("user_stats").as("s")).build()
        );

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.CTE_COLUMN_ALIAS_COUNT_MISMATCH));
    }

    @Test
    void validate_acceptsMatchingCteColumnAliasCount() {
        Query query = with(
            cte("user_stats",
                select(col("id"), col("status")).from(tbl("users")).build()
            ).columnAliases("id", "status")
        ).body(
            select(col("s", "id")).from(tbl("user_stats").as("s")).build()
        );

        var result = validator.validate(query);
        assertTrue(result.ok());
    }

    @Test
    void validate_reportsDuplicateCteNamesInWithBlock() {
        Query query = with(
            cte("dup", select(col("id")).from(tbl("users")).build()),
            cte("dup", select(col("id")).from(tbl("orders")).build())
        ).body(
            select(col("d", "id")).from(tbl("dup").as("d")).build()
        );

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DUPLICATE_CTE_NAME));
    }

    @Test
    void validate_reportsDuplicateCteNamesCaseInsensitive() {
        Query query = with(
            cte("dup", select(col("id")).from(tbl("users")).build()),
            cte("DUP", select(col("id")).from(tbl("orders")).build())
        ).body(
            select(col("d", "id")).from(tbl("dup").as("d")).build()
        );

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DUPLICATE_CTE_NAME));
    }

    @Test
    void validate_reportsNonRecursiveCteSelfReference() {
        Query query = with(
            cte("r",
                select(lit(1)).from(tbl("r")).build()
            ).columnAliases("n")
        ).body(
            select(star()).from(tbl("users").as("u")).build()
        );

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.CTE_RECURSION_NOT_ALLOWED));
    }

    @Test
    void validate_acceptsRecursiveCteSelfReference() {
        Query query = with(
            cte("r",
                select(lit(1)).from(tbl("r")).build()
            ).columnAliases("n")
        )
            .recursive(true)
            .body(
                select(star()).from(tbl("users").as("u")).build()
            );

        var result = validator.validate(query);
        assertTrue(result.problems().stream()
            .noneMatch(p -> p.code() == ValidationProblem.Code.CTE_RECURSION_NOT_ALLOWED
                || p.code() == ValidationProblem.Code.TABLE_NOT_FOUND));
    }

    @Test
    void validate_reportsRecursiveCteThatIsNotSetOperation() {
        Query query = with(
            cte("r",
                select(lit(1)).from(tbl("r")).build()
            ).columnAliases("n")
        )
            .recursive(true)
            .body(select(star()).from(tbl("users").as("u")).build());

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.CTE_RECURSIVE_STRUCTURE_INVALID));
    }

    @Test
    void validate_reportsRecursiveCteUsingNonUnionSetOperator() {
        Query query = with(
            cte("r",
                select(lit(1))
                    .build()
                    .intersect(select(lit(2)).from(tbl("r")).build())
            ).columnAliases("n")
        )
            .recursive(true)
            .body(select(star()).from(tbl("users").as("u")).build());

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.CTE_RECURSIVE_STRUCTURE_INVALID));
    }

    @Test
    void validate_reportsRecursiveCteWithoutRecursiveTermReference() {
        Query query = with(
            cte("r",
                select(lit(1)).from(tbl("r")).build()
                    .union(select(lit(2)).build())
            ).columnAliases("n")
        )
            .recursive(true)
            .body(select(star()).from(tbl("users").as("u")).build());

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.CTE_RECURSIVE_STRUCTURE_INVALID));
    }

    @Test
    void validate_reportsRecursiveCteProjectionArityMismatch() {
        Query query = with(
            cte("r",
                select(lit(1)).build()
                    .union(select(lit(2), lit(3)).from(tbl("r")).build())
            ).columnAliases("n")
        )
            .recursive(true)
            .body(select(star()).from(tbl("users").as("u")).build());

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.CTE_RECURSIVE_STRUCTURE_INVALID));
    }

    @Test
    void validate_acceptsWellFormedRecursiveCteStructure() {
        Query query = with(
            cte("r",
                select(lit(1)).build()
                    .union(
                        select(col("r", "n").add(lit(1)))
                            .from(tbl("r").as("r")).build()
                    )
            ).columnAliases("n")
        )
            .recursive(true)
            .body(select(star()).from(tbl("users").as("u")).build());

        var result = validator.validate(query);
        assertTrue(result.problems().stream()
            .noneMatch(p -> p.code() == ValidationProblem.Code.CTE_RECURSIVE_STRUCTURE_INVALID));
    }

    @Test
    void validate_reportsRecursiveCteTypeMismatchBetweenAnchorAndRecursiveTerm() {
        Query query = with(
            cte("r",
                select(lit(1)).build()
                    .union(
                        select(lit("x")).from(tbl("r").as("r")).build()
                    )
            ).columnAliases("n")
        )
            .recursive(true)
            .body(select(star()).from(tbl("users").as("u")).build());

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.CTE_RECURSIVE_TYPE_MISMATCH));
    }

    @Test
    void validate_acceptsRecursiveCteComparableNumericTypesBetweenTerms() {
        Query query = with(
            cte("r",
                select(lit(1)).build()
                    .union(
                        select(lit(2L)).from(tbl("r").as("r")).build()
                    )
            ).columnAliases("n")
        )
            .recursive(true)
            .body(select(star()).from(tbl("users").as("u")).build());

        var result = validator.validate(query);
        assertTrue(result.problems().stream()
            .noneMatch(p -> p.code() == ValidationProblem.Code.CTE_RECURSIVE_TYPE_MISMATCH));
    }

    @Test
    void validate_reportsSetOperationColumnCountMismatch() {
        Query query = select(lit(1)).build().union(select(lit(2), lit(3)).build());

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.SET_OPERATION_COLUMN_COUNT_MISMATCH));
    }

    @Test
    void validate_reportsSetOperationTypeMismatch() {
        Query query = select(lit(1)).build().union(select(lit("x")).build());

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream().anyMatch(p -> p.code() == ValidationProblem.Code.TYPE_MISMATCH));
    }

    @Test
    void validate_reportsInvalidSetOperationOrderByExpression() {
        Query query = select(lit(1)).build()
            .union(select(lit(2)).build())
            .orderBy(order(lit(3)));

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.SET_OPERATION_ORDER_BY_INVALID));
    }

    @Test
    void validate_acceptsSetOperationOrderByProjectedExpression() {
        Query query = select(lit(1)).build()
            .union(select(lit(2)).build())
            .orderBy(order(lit(1)));

        var result = validator.validate(query);
        assertTrue(result.ok());
    }

    @Test
    void validate_reportsSetOperationOrderByExpressionWhenOutputShapeIsNotExpressionOnly() {
        Query query = select(star())
            .from(tbl("users").as("u")).build()
            .union(select(star()).from(tbl("orders").as("o")).build())
            .orderBy(order(lit(1)));

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.SET_OPERATION_ORDER_BY_INVALID));
    }

    @Test
    void validate_acceptsSetOperationOrderByOrdinalWhenOutputShapeIsNotExpressionOnly() {
        Query query = select(star())
            .from(tbl("users").as("u")).build()
            .union(select(star()).from(tbl("orders").as("o")).build())
            .orderBy(order(1));

        var result = validator.validate(query);
        assertTrue(result.ok());
    }

    @Test
    void validate_reportsInvalidOrderByOrdinalInSelectQuery() {
        Query query = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .orderBy(order(2))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.ORDER_BY_INVALID_ORDINAL));
    }

    @Test
    void validate_reportsOrderByOrdinalReferencingStarSelectItem() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .orderBy(order(1))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.ORDER_BY_INVALID_ORDINAL));
    }

    @Test
    void validate_reportsInvalidZeroOrderByOrdinalInSelectQuery() {
        Query query = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .orderBy(order(0))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.ORDER_BY_INVALID_ORDINAL));
    }

    @Test
    void validate_reportsInvalidOrderByOrdinalInCompositeQuery() {
        Query query = select(lit(1)).build()
            .union(select(lit(2)).build())
            .orderBy(order(2));

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.ORDER_BY_INVALID_ORDINAL));
    }

    @Test
    void validate_acceptsValidOrderByOrdinal() {
        Query query = select(col("u", "id"), col("u", "status"))
            .from(tbl("users").as("u"))
            .orderBy(order(2))
            .build();

        var result = validator.validate(query);
        assertTrue(result.ok());
    }

    @Test
    void validate_reportsInvalidGroupByOrdinal() {
        Query query = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .groupBy(group(2))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.GROUP_BY_INVALID_ORDINAL));
    }

    @Test
    void validate_reportsGroupByOrdinalReferencingStarSelectItem() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .groupBy(group(1))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.GROUP_BY_INVALID_ORDINAL));
    }

    @Test
    void validate_reportsInvalidZeroGroupByOrdinal() {
        Query query = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .groupBy(group(0))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.GROUP_BY_INVALID_ORDINAL));
    }

    @Test
    void validate_reportsInvalidNestedGroupByOrdinal() {
        Query query = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .groupBy(groupingSets(groupingSet(group(2))))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.GROUP_BY_INVALID_ORDINAL));
    }

    @Test
    void validate_acceptsValidGroupByOrdinal() {
        Query query = select(col("u", "status"), func("count", starArg()))
            .from(tbl("users").as("u"))
            .groupBy(group(1))
            .build();

        var result = validator.validate(query);
        assertTrue(result.ok());
    }

    @Test
    void validate_reportsGroupedSelectNonAggregatedExpression() {
        Query query = select(col("u", "name"), col("u", "status"))
            .from(tbl("users").as("u"))
            .groupBy(group(col("u", "name")))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream().anyMatch(p -> p.code() == ValidationProblem.Code.AGGREGATION_MISUSE));
    }

    @Test
    void validate_acceptsGroupedSelectWithAggregate() {
        Query query = select(col("u", "status"), func("count", starArg()))
            .from(tbl("users").as("u"))
            .groupBy(group(col("u", "status")))
            .build();

        var result = validator.validate(query);
        assertTrue(result.ok());
    }

    @Test
    void validate_reportsHavingWithoutGroupOrAggregate() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .having(col("u", "status").eq(lit("active")))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream().anyMatch(p -> p.code() == ValidationProblem.Code.AGGREGATION_MISUSE));
    }

    @Test
    void validate_reportsHavingWithNonGroupedColumnOutsideAggregate() {
        Query query = select(col("u", "status"), func("count", starArg()))
            .from(tbl("users").as("u"))
            .groupBy(group(col("u", "status")))
            .having(col("u", "name").eq(lit("x")))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream().anyMatch(p -> p.code() == ValidationProblem.Code.AGGREGATION_MISUSE));
    }

    @Test
    void validate_acceptsHavingWithAggregatePredicate() {
        Query query = select(col("u", "status"), func("count", starArg()).as("cnt"))
            .from(tbl("users").as("u"))
            .groupBy(group(col("u", "status")))
            .having(func("count", starArg()).gt(lit(1)))
            .build();

        var result = validator.validate(query);
        assertTrue(result.ok());
    }

    @Test
    void validate_acceptsKnownFunctionSignatures() {
        Query query = select(
            func("lower", arg(col("u", "name"))),
            func("sum", arg(col("u", "age"))),
            func("count", starArg())
        ).from(tbl("users").as("u"))
            .build();

        var result = validator.validate(query);
        assertTrue(result.ok());
    }

    @Test
    void validate_reportsFunctionArityMismatch() {
        Query query = select(
            func("lower", arg(col("u", "name")), arg(col("u", "status")))
        ).from(tbl("users").as("u"))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.FUNCTION_SIGNATURE_MISMATCH));
    }

    @Test
    void validate_reportsFunctionTypeMismatch() {
        Query query = select(
            func("lower", arg(col("u", "age")))
        ).from(tbl("users").as("u"))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.FUNCTION_SIGNATURE_MISMATCH));
    }

    @Test
    void validate_includesStructuredDiagnosticContext() {
        Query query = select(
            func("lower", arg(col("u", "age")))
        ).from(tbl("users").as("u"))
            .build();

        var result = validator.validate(query);
        var problem = result.problems().stream()
            .filter(p -> p.code() == ValidationProblem.Code.FUNCTION_SIGNATURE_MISMATCH)
            .findFirst()
            .orElseThrow();

        assertEquals("FunctionExpr", problem.nodeKind());
        assertEquals("function.call", problem.clausePath());
    }

    @Test
    void validate_usesCustomFunctionCatalog() {
        FunctionCatalog catalog = name -> "lower".equalsIgnoreCase(name)
            ? java.util.Optional.of(FunctionSignature.of(1, 1, FunctionArgKind.NUMERIC_EXPR))
            : java.util.Optional.empty();
        var customValidator = SchemaQueryValidator.of(SCHEMA, catalog);

        Query query = select(
            func("lower", arg(col("u", "name")))
        ).from(tbl("users").as("u"))
            .build();

        var result = customValidator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.FUNCTION_SIGNATURE_MISMATCH));
    }

    @Test
    void validate_usesCustomFunctionCatalogForAggregationSemantics() {
        FunctionCatalog catalog = name -> "my_agg".equalsIgnoreCase(name)
            ? java.util.Optional.of(FunctionSignature.ofAggregate(1, 1, FunctionArgKind.ANY_EXPR))
            : java.util.Optional.empty();
        var customValidator = SchemaQueryValidator.of(SCHEMA, catalog);

        Query query = select(
            col("u", "status"),
            func("my_agg", arg(col("u", "name")))
        )
            .from(tbl("users").as("u"))
            .groupBy(group(col("u", "status")))
            .build();

        var result = customValidator.validate(query);
        assertTrue(result.ok());
    }

    @Test
    void validate_reportsAggregationMisuseWhenCustomFunctionIsNotAggregate() {
        FunctionCatalog catalog = name -> "my_agg".equalsIgnoreCase(name)
            ? java.util.Optional.of(FunctionSignature.of(1, 1, FunctionArgKind.ANY_EXPR))
            : java.util.Optional.empty();
        var customValidator = SchemaQueryValidator.of(SCHEMA, catalog);

        Query query = select(
            col("u", "status"),
            func("my_agg", arg(col("u", "name")))
        )
            .from(tbl("users").as("u"))
            .groupBy(group(col("u", "status")))
            .build();

        var result = customValidator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.AGGREGATION_MISUSE));
    }

    @Test
    void validate_reportsFunctionSignatureMismatchWhenAnyExprReceivesStarArg() {
        FunctionCatalog catalog = name -> "f_any".equalsIgnoreCase(name)
            ? java.util.Optional.of(FunctionSignature.of(1, 1, FunctionArgKind.ANY_EXPR))
            : java.util.Optional.empty();
        var customValidator = SchemaQueryValidator.of(SCHEMA, catalog);

        Query query = select(func("f_any", starArg())).from(tbl("users").as("u")).build();
        var result = customValidator.validate(query);

        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.FUNCTION_SIGNATURE_MISMATCH));
    }

    @Test
    void validate_acceptsFunctionSignatureStarOrExprWithStarArg() {
        FunctionCatalog catalog = name -> "f_star_or_expr".equalsIgnoreCase(name)
            ? java.util.Optional.of(FunctionSignature.of(1, 1, FunctionArgKind.STAR_OR_EXPR))
            : java.util.Optional.empty();
        var customValidator = SchemaQueryValidator.of(SCHEMA, catalog);

        Query query = select(func("f_star_or_expr", starArg())).from(tbl("users").as("u")).build();
        var result = customValidator.validate(query);

        assertTrue(result.ok());
    }

    @Test
    void validate_reportsSetOperationOrderByInvalidWhenProjectionIsNotExpressionOnly() {
        Query query = select(star()).from(tbl("users").as("u")).build()
            .union(select(star()).from(tbl("users").as("u2")).build())
            .orderBy(order(col("u", "id")));

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.SET_OPERATION_ORDER_BY_INVALID));
    }

    @Test
    void validate_reportsJoinUsingInvalidColumnWhenRightSourceHasNoAliasKey() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .join(inner(tbl(select(col("id")).from(tbl("orders")).build())).using("id"))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.JOIN_USING_INVALID_COLUMN));
    }

    @Test
    void validate_acceptsSettingsFactoryWithDefaultBehavior() {
        var customValidator = SchemaQueryValidator.of(SCHEMA, SchemaValidationSettings.defaults());
        Query query = select(star()).from(tbl("users").as("u")).build();

        var result = customValidator.validate(query);
        assertTrue(result.ok());
    }

    @Test
    void validate_appliesAdditionalRulesFromDialectSettings() {
        SchemaValidationRule<SelectQuery> forcedRule = new SchemaValidationRule<>() {
            @Override
            public Class<SelectQuery> nodeType() {
                return SelectQuery.class;
            }

            @Override
            public void validate(SelectQuery node, io.sqm.validate.schema.internal.SchemaValidationContext context) {
                context.addProblem(
                    ValidationProblem.Code.TYPE_MISMATCH,
                    "forced dialect validation error",
                    node,
                    "dialect.custom"
                );
            }
        };

        SchemaValidationDialect dialect = new SchemaValidationDialect() {
            @Override
            public String name() {
                return "test";
            }

            @Override
            public List<SchemaValidationRule<? extends io.sqm.core.Node>> additionalRules() {
                return List.of(forcedRule);
            }
        };

        var customValidator = SchemaQueryValidator.of(SCHEMA, dialect);
        Query query = select(star()).from(tbl("users").as("u")).build();
        var result = customValidator.validate(query);

        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.TYPE_MISMATCH
                && "dialect.custom".equals(p.clausePath())));
    }

    @Test
    void validate_reportsMissingWindowReference() {
        Query query = select(
            func("sum", arg(col("u", "age"))).over("missing_window")
        ).from(tbl("users").as("u"))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.WINDOW_NOT_FOUND));
    }

    @Test
    void validate_reportsMissingBaseWindowReference() {
        Query query = select(
            func("sum", arg(col("u", "age"))).over(over("missing_window", orderBy(order(col("u", "id")))))
        ).from(tbl("users").as("u"))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.WINDOW_NOT_FOUND));
    }

    @Test
    void validate_acceptsDeclaredWindowReference() {
        Query query = select(
            func("sum", arg(col("u", "age"))).over("w")
        )
            .from(tbl("users").as("u"))
            .window(window("w", partition(col("u", "status"))))
            .build();

        var result = validator.validate(query);
        assertTrue(result.ok());
    }

    @Test
    void validate_reportsDuplicateWindowNames() {
        Query query = select(
            func("sum", arg(col("u", "age"))).over("w")
        )
            .from(tbl("users").as("u"))
            .window(
                window("w", partition(col("u", "status"))),
                window("w", partition(col("u", "id")))
            )
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DUPLICATE_WINDOW_NAME));
    }

    @Test
    void validate_reportsDuplicateWindowNamesCaseInsensitive() {
        Query query = select(
            func("sum", arg(col("u", "age"))).over("w")
        )
            .from(tbl("users").as("u"))
            .window(
                window("w", partition(col("u", "status"))),
                window("W", partition(col("u", "id")))
            )
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DUPLICATE_WINDOW_NAME));
    }

    @Test
    void validate_acceptsDistinctWindowNames() {
        Query query = select(
            func("sum", arg(col("u", "age"))).over("w1"),
            func("avg", arg(col("u", "age"))).over("w2")
        )
            .from(tbl("users").as("u"))
            .window(
                window("w1", partition(col("u", "status"))),
                window("w2", partition(col("u", "id")))
            )
            .build();

        var result = validator.validate(query);
        assertTrue(result.ok());
    }

    @Test
    void validate_reportsMissingBaseWindowInWindowDefinition() {
        Query query = select(
            func("sum", arg(col("u", "age"))).over("w1")
        )
            .from(tbl("users").as("u"))
            .window(
                WindowDef.of("w1", OverSpec.def("missing", orderBy(order(col("u", "id"))), null, null))
            )
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.WINDOW_NOT_FOUND));
    }

    @Test
    void validate_reportsWindowInheritanceCycle() {
        Query query = select(
            func("sum", arg(col("u", "age"))).over("w1")
        )
            .from(tbl("users").as("u"))
            .window(
                WindowDef.of("w1", OverSpec.def("w2", orderBy(order(col("u", "id"))), null, null)),
                WindowDef.of("w2", OverSpec.def("w1", orderBy(order(col("u", "status"))), null, null))
            )
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.WINDOW_INHERITANCE_CYCLE));
    }

    @Test
    void validate_reportsRangeOffsetFrameWhenWindowOrderByResolutionFallsBackToCycleGuard() {
        Query query = select(
            func("sum", arg(col("u", "age"))).over("w1")
        )
            .from(tbl("users").as("u"))
            .window(
                WindowDef.of("w1", OverSpec.def("w2", null, range(preceding(1)), null)),
                WindowDef.of("w2", OverSpec.def("w1", null, null, null))
            )
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.WINDOW_FRAME_INVALID));
    }

    @Test
    void validate_reportsSelfWindowInheritanceCycle() {
        Query query = select(
            func("sum", arg(col("u", "age"))).over("w1")
        )
            .from(tbl("users").as("u"))
            .window(
                WindowDef.of("w1", OverSpec.def("w1", orderBy(order(col("u", "id"))), null, null))
            )
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.WINDOW_INHERITANCE_CYCLE));
    }

    @Test
    void validate_acceptsWindowInheritanceChain() {
        Query query = select(
            func("sum", arg(col("u", "age"))).over("w2")
        )
            .from(tbl("users").as("u"))
            .window(
                window("w1", partition(col("u", "status"))),
                WindowDef.of("w2", OverSpec.def("w1", orderBy(order(col("u", "id"))), null, null))
            )
            .build();

        var result = validator.validate(query);
        assertTrue(result.ok());
    }

    @Test
    void validate_reportsWindowFrameNonNumericBoundExpression() {
        Query query = select(
            func("sum", arg(col("u", "age"))).over(over(rows(preceding(lit("x")))))
        ).from(tbl("users").as("u"))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.WINDOW_FRAME_INVALID));
    }

    @Test
    void validate_reportsWindowFrameNegativeBoundExpression() {
        Query query = select(
            func("sum", arg(col("u", "age"))).over(over(rows(preceding(lit(-1)))))
        ).from(tbl("users").as("u"))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.WINDOW_FRAME_INVALID));
    }

    @Test
    void validate_reportsWindowFrameInvalidBetweenBoundOrderInlineOver() {
        Query query = select(
            func("sum", arg(col("u", "age")))
                .over(over(rows(following(1), preceding(1))))
        ).from(tbl("users").as("u"))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.WINDOW_FRAME_INVALID));
    }

    @Test
    void validate_reportsWindowFrameInvalidBetweenBoundOrderNamedWindow() {
        Query query = select(
            func("sum", arg(col("u", "age"))).over("w")
        )
            .from(tbl("users").as("u"))
            .window(window("w", partition(col("u", "status")), orderBy(order(col("u", "id"))), rows(following(1), currentRow())))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.WINDOW_FRAME_INVALID));
    }

    @Test
    void validate_acceptsWindowFrameValidBetweenBounds() {
        Query query = select(
            func("sum", arg(col("u", "age")))
                .over(over(rows(unboundedPreceding(), currentRow())))
        ).from(tbl("users").as("u"))
            .build();

        var result = validator.validate(query);
        assertTrue(result.problems().stream()
            .noneMatch(p -> p.code() == ValidationProblem.Code.WINDOW_FRAME_INVALID));
    }

    @Test
    void validate_reportsRangeOffsetFrameWithoutOrderBy() {
        Query query = select(
            func("sum", arg(col("u", "age")))
                .over(over(range(preceding(1))))
        ).from(tbl("users").as("u"))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.WINDOW_FRAME_INVALID));
    }

    @Test
    void validate_reportsRangeOffsetFrameWithMultipleOrderByItems() {
        Query query = select(
            func("sum", arg(col("u", "age")))
                .over(over(orderBy(order(col("u", "id")), order(col("u", "age"))), range(preceding(1))))
        ).from(tbl("users").as("u"))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.WINDOW_FRAME_INVALID));
    }

    @Test
    void validate_acceptsRangeOffsetFrameWithSingleOrderByItem() {
        Query query = select(
            func("sum", arg(col("u", "age")))
                .over(over(orderBy(order(col("u", "id"))), range(preceding(1))))
        ).from(tbl("users").as("u"))
            .build();

        var result = validator.validate(query);
        assertTrue(result.problems().stream()
            .noneMatch(p -> p.code() == ValidationProblem.Code.WINDOW_FRAME_INVALID));
    }

    @Test
    void validate_reportsGroupsFrameWithDecimalOffset() {
        Query query = select(
            func("sum", arg(col("u", "age")))
                .over(over(groups(preceding(lit(1.5)))))
        ).from(tbl("users").as("u"))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.WINDOW_FRAME_INVALID));
    }

    @Test
    void validate_doesNotApplyDistinctOnSemanticsWithoutPostgresDialect() {
        Query query = select(col("u", "status"), col("u", "id"))
            .from(tbl("users").as("u"))
            .distinct(distinctOn(col("u", "status")))
            .build();

        var result = validator.validate(query);
        assertFalse(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DISTINCT_ON_ORDER_BY_MISMATCH));
    }

    @Test
    void validate_doesNotApplyDistinctOnPrefixSemanticsWithoutPostgresDialect() {
        Query query = select(col("u", "status"), col("u", "id"))
            .from(tbl("users").as("u"))
            .distinct(distinctOn(col("u", "status")))
            .orderBy(order(col("u", "id")))
            .build();

        var result = validator.validate(query);
        assertFalse(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.DISTINCT_ON_ORDER_BY_MISMATCH));
    }

    @Test
    void validate_acceptsDistinctOnWithMatchingOrderByPrefix() {
        Query query = select(col("u", "status"), col("u", "id"))
            .from(tbl("users").as("u"))
            .distinct(distinctOn(col("u", "status")))
            .orderBy(order(col("u", "status")), order(col("u", "id")))
            .build();

        var result = validator.validate(query);
        assertTrue(result.ok());
    }

    @Test
    void validate_acceptsDistinctOnWithMatchingOrderByOrdinal() {
        Query query = select(col("u", "status"), col("u", "id"))
            .from(tbl("users").as("u"))
            .distinct(distinctOn(col("u", "status")))
            .orderBy(order(1), order(2))
            .build();

        var result = validator.validate(query);
        assertTrue(result.ok());
    }

    @Test
    void validate_reportsTypeMismatchForUnaryPredicate() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .where(unary(lit(1)))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.TYPE_MISMATCH
                && "predicate.unary".equals(p.clausePath())));
    }

    @Test
    void validate_acceptsBooleanUnaryPredicate() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .where(unary(lit(true)))
            .build();

        var result = validator.validate(query);
        assertTrue(result.ok());
    }

    @Test
    void validate_reportsInvalidLimitType() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .limit(col("u", "status"))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.LIMIT_OFFSET_INVALID));
    }

    @Test
    void validate_reportsInvalidOffsetType() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .offset(col("u", "status"))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.LIMIT_OFFSET_INVALID));
    }

    @Test
    void validate_reportsInvalidLimitTypeForFunctionReturnType() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .limit(func("lower", arg(col("u", "name"))))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.LIMIT_OFFSET_INVALID));
    }

    @Test
    void validate_acceptsNumericLimitForFunctionReturnType() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .limit(func("length", arg(col("u", "name"))))
            .build();

        var result = validator.validate(query);
        assertTrue(result.problems().stream()
            .noneMatch(p -> p.code() == ValidationProblem.Code.LIMIT_OFFSET_INVALID));
    }

    @Test
    void validate_acceptsNumericLimitForArithmeticExpression() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .limit(col("u", "age").add(lit(1)))
            .build();

        var result = validator.validate(query);
        assertTrue(result.problems().stream()
            .noneMatch(p -> p.code() == ValidationProblem.Code.LIMIT_OFFSET_INVALID));
    }

    @Test
    void validate_reportsTypeMismatchForCastTargetType() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .where(col("u", "age").eq(lit("10").cast(type("text"))))
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.TYPE_MISMATCH));
    }

    @Test
    void validate_acceptsCompatibleTypeForCastTargetType() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .where(col("u", "age").eq(lit("10").cast(type("int"))))
            .build();

        var result = validator.validate(query);
        assertTrue(result.ok());
    }

    @Test
    void validate_acceptsNumericLimitOffsetExpressions() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .limit(col("u", "age"))
            .offset(lit(1))
            .build();

        var result = validator.validate(query);
        assertTrue(result.ok());
    }

    @Test
    void validate_reportsInvalidCompositeLimitType() {
        Query query = select(lit(1)).build()
            .union(select(lit(2)).build())
            .limit(lit("x"));

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.LIMIT_OFFSET_INVALID));
    }

    @Test
    void validate_usesCustomFunctionReturnTypeForInference() {
        FunctionCatalog catalog = name -> "foo".equalsIgnoreCase(name)
            ? java.util.Optional.of(FunctionSignature.of(1, 1, CatalogType.STRING, FunctionArgKind.ANY_EXPR))
            : java.util.Optional.empty();
        var customValidator = SchemaQueryValidator.of(SCHEMA, catalog);

        Query query = select(star())
            .from(tbl("users").as("u"))
            .limit(func("foo", arg(col("u", "id"))))
            .build();

        var result = customValidator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.LIMIT_OFFSET_INVALID));
    }

    @Test
    void validate_skipsStrictMismatchChecksForUnknownTypeColumns() {
        CatalogSchema unknownSchema = CatalogSchema.of(
            CatalogTable.of("public", "events", CatalogColumn.of("payload", CatalogType.UNKNOWN))
        );
        var unknownValidator = SchemaQueryValidator.of(unknownSchema);

        Query query = select(star())
            .from(tbl("events").as("e"))
            .where(col("e", "payload").eq(lit("x")))
            .limit(col("e", "payload"))
            .build();

        var result = unknownValidator.validate(query);
        assertTrue(result.problems().stream()
            .noneMatch(p -> p.code() == ValidationProblem.Code.TYPE_MISMATCH
                || p.code() == ValidationProblem.Code.LIMIT_OFFSET_INVALID));
    }

    @Test
    void validate_reportsMissingLockTargetAlias() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .lockFor(update(), ofTables("missing_alias"), false, false)
            .build();

        var result = validator.validate(query);
        assertFalse(result.ok());
        assertTrue(result.problems().stream()
            .anyMatch(p -> p.code() == ValidationProblem.Code.LOCK_TARGET_NOT_FOUND));
    }

    @Test
    void validate_acceptsExistingLockTargetAlias() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .join(inner(tbl("orders").as("o")).on(col("o", "user_id").eq(col("u", "id"))))
            .lockFor(update(), ofTables("u", "o"), false, false)
            .build();

        var result = validator.validate(query);
        assertTrue(result.ok());
    }

    @Test
    void validate_acceptsLockingClauseWithoutOfTargets() {
        Query query = select(star())
            .from(tbl("users").as("u"))
            .lockFor(update(), List.of(), false, false)
            .build();

        var result = validator.validate(query);
        assertTrue(result.ok());
    }
}

