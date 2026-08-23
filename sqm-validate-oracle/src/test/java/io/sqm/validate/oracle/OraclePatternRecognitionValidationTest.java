package io.sqm.validate.oracle;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.core.*;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.api.ValidationResult;
import io.sqm.validate.schema.SchemaStatementValidator;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OraclePatternRecognitionValidationTest {
    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "sales",
            CatalogColumn.of("account_id", CatalogType.LONG),
            CatalogColumn.of("event_time", CatalogType.TIMESTAMP),
            CatalogColumn.of("amount", CatalogType.DECIMAL),
            CatalogColumn.of("status", CatalogType.STRING)
        )
    );

    @Test
    void acceptsCompleteOraclePatternRecognition() {
        var table = matchRecognize(tbl("sales"))
            .partitionBy(col("account_id"))
            .orderBy(col("event_time").asc())
            .measure(running(func("sum", patternColumn("A", "amount"))), "running_total")
            .measure(first(patternColumn("A", "amount")), "first_amount")
            .measure(classifier(), "label")
            .measure(matchNumber(), "match_no")
            .allRowsPerMatch()
            .afterMatchSkip(skipToFirst("B"))
            .pattern(patternSequence(patternVar("A"), oneOrMore(patternVar("B"))))
            .subset("AB", "A", "B")
            .define("A", patternColumn("A", "amount").gt(0))
            .define("B", patternColumn("B", "amount").gt(prev(patternColumn("B", "amount"))))
            .as("mr")
            .build();

        assertTrue(validate(table).ok(), () -> validate(table).problems().toString());
    }

    @Test
    void rejectsMatchRecognizeBeforeOracle12_1() {
        var result = validator(SqlDialectVersion.of(11, 2)).validate(select(star()).from(minimal()).build());

        assertTrue(has(result, ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED, "from.matchRecognize"));
    }

    @Test
    void rejectsDuplicateMeasuresDefinitionsAndSubsets() {
        var table = matchRecognize(tbl("sales"))
            .measure(matchNumber(), "n")
            .measure(classifier(), "N")
            .pattern(patternSequence(patternVar("A"), patternVar("B")))
            .subset("AB", "A", "A")
            .subset("ab", "A", "B")
            .define("A", patternColumn("A", "amount").gt(0))
            .define("a", patternColumn("A", "amount").gt(1))
            .define("B", patternColumn("B", "amount").gt(0))
            .build();

        var result = validate(table);

        assertTrue(hasMessage(result, "Duplicate measure alias"));
        assertTrue(hasMessage(result, "Duplicate pattern definition"));
        assertTrue(hasMessage(result, "Duplicate subset name"));
        assertTrue(hasMessage(result, "Duplicate subset member"));
    }

    @Test
    void requiresExactlyOneDefinitionForEveryPrimaryVariable() {
        var table = matchRecognize(tbl("sales"))
            .pattern(patternSequence(patternVar("A"), patternVar("B")))
            .define("A", patternColumn("A", "amount").gt(0))
            .define("C", patternColumn("C", "amount").gt(0))
            .build();

        var result = validate(table);

        assertTrue(hasMessage(result, "Definition does not name a primary pattern variable: C"));
        assertTrue(hasMessage(result, "Primary pattern variable requires an explicit DEFINE item: B"));
    }

    @Test
    void validatesSubsetNamesAndMembership() {
        var table = matchRecognize(tbl("sales"))
            .pattern(patternSequence(patternVar("A"), patternVar("B")))
            .subset("A", "A")
            .subset("X", "A", "C")
            .define("A", patternColumn("A", "amount").gt(0))
            .define("B", patternColumn("B", "amount").gt(0))
            .build();

        var result = validate(table);

        assertTrue(hasMessage(result, "Subset name collides with a primary pattern variable"));
        assertTrue(hasMessage(result, "Subset member is not a primary pattern variable: C"));
    }

    @Test
    void validatesSkipTargetsAndScopedVariables() {
        var table = matchRecognize(tbl("sales"))
            .measure(patternColumn("Z", "amount"), "unknown_value")
            .measure(classifier(id("A")), "label")
            .afterMatchSkip(skipToFirst("AB"))
            .pattern(patternSequence(patternVar("A"), patternVar("B")))
            .subset("AB", "A", "B")
            .define("A", patternColumn("A", "amount").gt(0))
            .define("B", patternColumn("B", "amount").gt(0))
            .build();

        var result = validate(table);

        assertTrue(hasMessage(result, "skip target must be a defined primary pattern variable"));
        assertTrue(hasMessage(result, "Unknown pattern variable: Z"));
        assertTrue(hasMessage(result, "CLASSIFIER does not accept a pattern-variable argument"));
    }

    @Test
    void appliesOracleIdentifierComparisonToPatternScope() {
        var quotedUpper = id("A", QuoteStyle.DOUBLE_QUOTE);
        var quotedLower = id("a", QuoteStyle.DOUBLE_QUOTE);
        var accepted = matchRecognize(tbl("sales"))
            .pattern(patternVar(quotedUpper))
            .define(patternDefinition(id("A"), patternColumn(quotedUpper, id("amount")).gt(0)))
            .build();
        var rejected = matchRecognize(tbl("sales"))
            .pattern(patternVar(quotedLower))
            .define(patternDefinition(id("A"), patternColumn(quotedLower, id("amount")).gt(0)))
            .build();

        assertFalse(hasMessage(validate(accepted), "does not name a primary"));
        assertTrue(hasMessage(validate(rejected), "does not name a primary"));
    }

    @Test
    void rejectsOracleEvaluationMisuse() {
        var finalDefinition = matchRecognize(tbl("sales"))
            .pattern(patternVar("A"))
            .define("A", finalValue(last(patternColumn("A", "amount"))).gt(0))
            .build();
        var unsupportedTarget = matchRecognize(tbl("sales"))
            .measure(finalValue(patternColumn("A", "amount")), "amount")
            .pattern(patternVar("A"))
            .define("A", patternColumn("A", "amount").gt(0))
            .build();
        var evaluatedPrev = matchRecognize(tbl("sales"))
            .measure(finalValue(prev(patternColumn("A", "amount"))), "amount")
            .pattern(patternVar("A"))
            .define("A", patternColumn("A", "amount").gt(0))
            .build();
        var nestedEvaluation = matchRecognize(tbl("sales"))
            .measure(prev(finalValue(last(patternColumn("A", "amount")))), "amount")
            .pattern(patternVar("A"))
            .define("A", patternColumn("A", "amount").gt(0))
            .build();

        assertTrue(hasMessage(validate(finalDefinition), "FINAL evaluation is not allowed in DEFINE"));
        assertTrue(hasMessage(validate(unsupportedTarget), "must wrap a supported navigation or aggregate"));
        assertTrue(hasMessage(validate(evaluatedPrev), "PREV and NEXT cannot use RUNNING or FINAL"));
        assertTrue(hasMessage(validate(nestedEvaluation), "PREV and NEXT cannot contain RUNNING or FINAL"));
    }

    @Test
    void rejectsInvalidNavigationOffsets() {
        var table = matchRecognize(tbl("sales"))
            .measure(prev(patternColumn("A", "amount"), lit(-1)), "negative")
            .measure(next(patternColumn("A", "amount"), lit(1.5)), "fractional")
            .measure(first(patternColumn("A", "amount"), lit("two")), "textual")
            .pattern(patternVar("A"))
            .define("A", patternColumn("A", "amount").gt(0))
            .build();

        var result = validate(table);

        assertTrue(result.problems().stream().filter(problem ->
            problem.message().contains("offset must be a non-negative integer")).count() >= 3);
    }

    @Test
    void rejectsUnsupportedAggregateFormsAndWindows() {
        var table = matchRecognize(tbl("sales"))
            .measure(func("array_agg", patternColumn("A", "amount")), "array_value")
            .measure(func("sum", patternColumn("A", "amount")).distinct(), "distinct_total")
            .measure(func("sum", patternColumn("A", "amount")).over(orderBy(col("event_time").asc())), "window_total")
            .pattern(patternVar("A"))
            .define("A", patternColumn("A", "amount").gt(0))
            .build();

        var result = validate(table);

        assertTrue(hasMessage(result, "Unsupported Oracle row-pattern aggregate: ARRAY_AGG"));
        assertTrue(hasMessage(result, "DISTINCT is not allowed in Oracle row-pattern aggregates"));
        assertTrue(hasMessage(result, "Window expressions are not allowed in Oracle MATCH_RECOGNIZE"));
    }

    @Test
    void rejectsNonOracleRowsPerMatchOptionsDuringValidation() {
        var table = matchRecognize(tbl("sales"))
            .rowsPerMatch(allRowsPerMatch(RowsPerMatch.EmptyMatchHandling.SHOW_EMPTY))
            .pattern(patternVar("A"))
            .define("A", patternColumn("A", "amount").gt(0))
            .build();

        assertTrue(has(validate(table), ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
            "from.matchRecognize.rowsPerMatch"));
    }

    @Test
    void rejectsRowPatternExpressionsOutsidePatternScope() {
        var query = select(
            patternColumn("A", "amount"),
            classifier(),
            matchNumber(),
            prev(col("amount")),
            running(first(col("amount")))
        ).from(tbl("sales")).build();

        var result = validator(SqlDialectVersion.of(19, 0)).validate(query);

        assertTrue(result.problems().stream().filter(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "expression.matchRecognize".equals(problem.clausePath())).count() >= 5);
    }

    @Test
    void resolvesPatternColumnsAgainstInputSchemaWithStablePaths() {
        var table = matchRecognize(tbl("sales"))
            .pattern(patternVar("A"))
            .define("A", patternColumn("A", "missing").gt(0))
            .build();

        assertTrue(has(validate(table), ValidationProblem.Code.COLUMN_NOT_FOUND,
            "from.matchRecognize.definitions[0]"));
    }

    @Test
    void infersOneRowOutputAndKeepsAllRowsOutputPartial() {
        var oneRow = matchRecognize(tbl("sales"))
            .partitionBy(col("account_id"))
            .measure(matchNumber(), "match_no")
            .pattern(patternVar("A"))
            .define("A", patternColumn("A", "amount").gt(0))
            .as("mr")
            .build();
        var allRows = PatternRecognitionTable.builder(oneRow)
            .allRowsPerMatch()
            .build();
        var known = select(col("mr", "account_id"), col("mr", "match_no")).from(oneRow).build();
        var unknownOne = select(col("mr", "unknown")).from(oneRow).build();
        var unknownAll = select(col("mr", "unknown")).from(allRows).build();

        assertFalse(has(validator(SqlDialectVersion.of(19, 0)).validate(known),
            ValidationProblem.Code.COLUMN_NOT_FOUND, "column.reference"));
        assertTrue(has(validator(SqlDialectVersion.of(19, 0)).validate(unknownOne),
            ValidationProblem.Code.COLUMN_NOT_FOUND, "column.reference"));
        assertFalse(has(validator(SqlDialectVersion.of(19, 0)).validate(unknownAll),
            ValidationProblem.Code.COLUMN_NOT_FOUND, "column.reference"));
    }

    @Test
    void reportsDuplicateOneRowOutputNames() {
        var table = matchRecognize(tbl("sales"))
            .partitionBy(col("account_id"), col("account_id"))
            .measure(matchNumber(), "account_id")
            .pattern(patternVar("A"))
            .define("A", patternColumn("A", "amount").gt(0))
            .build();

        assertTrue(hasMessage(validate(table), "Duplicate inferred output column"));
    }

    private static PatternRecognitionTable minimal() {
        return matchRecognize(tbl("sales"))
            .pattern(patternVar("A"))
            .define("A", patternColumn("A", "amount").gt(0))
            .build();
    }

    private static ValidationResult validate(PatternRecognitionTable table) {
        return validator(SqlDialectVersion.of(19, 0)).validate(select(star()).from(table).build());
    }

    private static SchemaStatementValidator validator(SqlDialectVersion version) {
        return SchemaStatementValidator.of(SCHEMA, OracleValidationDialect.of(version));
    }

    private static boolean has(ValidationResult result, ValidationProblem.Code code, String path) {
        return result.problems().stream().anyMatch(problem ->
            problem.code() == code && path.equals(problem.clausePath()));
    }

    private static boolean hasMessage(ValidationResult result, String fragment) {
        return result.problems().stream().anyMatch(problem -> problem.message().contains(fragment));
    }
}
