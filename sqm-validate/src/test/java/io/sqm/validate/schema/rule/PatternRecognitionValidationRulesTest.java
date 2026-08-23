package io.sqm.validate.schema.rule;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.core.Expression;
import io.sqm.core.PatternColumnExpr;
import io.sqm.core.PatternRecognitionTable;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.VersionedDialectCapabilities;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.SchemaStatementValidator;
import io.sqm.validate.schema.SchemaValidationSettings;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static io.sqm.validate.schema.internal.SchemaValidationContext.PatternExpressionKind.MEASURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatternRecognitionValidationRulesTest {
    private static final SqlDialectVersion VERSION = SqlDialectVersion.of(1);
    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "sales",
            CatalogColumn.of("account_id", CatalogType.LONG),
            CatalogColumn.of("amount", CatalogType.DECIMAL)
        )
    );

    @Test
    void featureRuleReportsUnsupportedAndAcceptsSupportedVersions() {
        var table = minimalTable();
        var unsupported = context();
        var supported = context();
        var unsupportedRule = featureRule(false);
        var supportedRule = featureRule(true);

        unsupportedRule.validate(table, unsupported);
        supportedRule.validate(table, supported);

        assertEquals(PatternRecognitionTable.class, unsupportedRule.nodeType());
        assertEquals(1, unsupported.problems().size());
        assertEquals(ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
            unsupported.problems().getFirst().code());
        assertEquals("from.matchRecognize", unsupported.problems().getFirst().clausePath());
        assertTrue(supported.problems().isEmpty());
    }

    @Test
    void scopeRuleRejectsEveryPatternOnlyExpressionOutsidePatternClauses() {
        var context = context();
        var rule = new PatternExpressionScopeValidationRule();
        var expressions = List.<Expression>of(
            patternColumn("A", "amount"),
            classifier(),
            matchNumber(),
            prev(col("amount")),
            running(first(col("amount")))
        );

        rule.validate(col("amount"), context);
        expressions.forEach(expression -> rule.validate(expression, context));

        assertEquals(Expression.class, rule.nodeType());
        assertEquals(expressions.size(), context.problems().size());
        assertTrue(context.problems().stream().allMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "expression.matchRecognize".equals(problem.clausePath())));

        context.pushPatternExpressionScope(MEASURE, "from.matchRecognize.measures[0]");
        assertTrue(context.inPatternExpressionScope());
        assertEquals(MEASURE, context.patternExpressionKind());
        assertEquals("from.matchRecognize.measures[0]", context.patternExpressionPath());
        rule.validate(patternColumn("A", "amount"), context);
        context.popPatternExpressionScope();

        assertFalse(context.inPatternExpressionScope());
        assertNull(context.patternExpressionKind());
        assertEquals(expressions.size(), context.problems().size());
    }

    @Test
    void columnRuleResolvesOnlyInsidePatternScope() {
        var context = context();
        var rule = new PatternColumnReferenceValidationRule();
        var known = patternColumn("A", "amount");
        var missing = patternColumn("A", "missing");
        context.pushScope();
        context.registerTableRef(tbl("sales"));

        rule.validate(missing, context);
        context.pushPatternExpressionScope(MEASURE, "from.matchRecognize.measures[0]");
        rule.validate(known, context);
        rule.validate(missing, context);
        context.popPatternExpressionScope();
        context.popScope();

        assertEquals(PatternColumnExpr.class, rule.nodeType());
        assertEquals(1, context.problems().size());
        assertEquals(ValidationProblem.Code.COLUMN_NOT_FOUND, context.problems().getFirst().code());
        assertEquals("from.matchRecognize.measures[0]", context.problems().getFirst().clausePath());
    }

    @Test
    void validatorTraversesPatternClausesAndRegistersOutputShape() {
        var invalidClause = matchRecognize(tbl("sales"))
            .pattern(patternVar("A"))
            .define("A", patternColumn("A", "missing").gt(0))
            .build();
        var oneRow = matchRecognize(tbl("sales"))
            .partitionBy(col("account_id"))
            .measure(matchNumber(), "match_no")
            .pattern(patternVar("A"))
            .define("A", patternColumn("A", "amount").gt(0))
            .as("mr")
            .build();
        var allRows = PatternRecognitionTable.builder(oneRow).allRowsPerMatch().build();
        var validator = validator();

        var clauseResult = validator.validate(select(star()).from(invalidClause).build());
        var knownOutput = validator.validate(
            select(col("mr", "account_id"), col("mr", "match_no")).from(oneRow).build()
        );
        var unknownOneRow = validator.validate(select(col("mr", "unknown")).from(oneRow).build());
        var unknownAllRows = validator.validate(select(col("mr", "unknown")).from(allRows).build());

        assertTrue(clauseResult.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.COLUMN_NOT_FOUND
                && "from.matchRecognize.definitions[0]".equals(problem.clausePath())));
        assertFalse(knownOutput.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.COLUMN_NOT_FOUND));
        assertTrue(unknownOneRow.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.COLUMN_NOT_FOUND));
        assertFalse(unknownAllRows.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.COLUMN_NOT_FOUND));
    }

    @Test
    void validatorDispatchesEveryPatternOnlyExpressionOutsidePatternScope() {
        var result = validator().validate(select(
            patternColumn("A", "amount"),
            classifier(),
            matchNumber(),
            prev(col("amount")),
            running(first(col("amount")))
        ).from(tbl("sales")).build());

        assertEquals(6, result.problems().stream().filter(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && "expression.matchRecognize".equals(problem.clausePath())).count());
    }

    private static PatternRecognitionFeatureValidationRule featureRule(boolean supported) {
        var capabilities = VersionedDialectCapabilities.builder(VERSION);
        if (supported) {
            capabilities.supports(SqlFeature.MATCH_RECOGNIZE);
        }
        return new PatternRecognitionFeatureValidationRule("test", VERSION, capabilities.build());
    }

    private static SchemaStatementValidator validator() {
        return SchemaStatementValidator.of(
            SCHEMA,
            SchemaValidationSettings.builder().addRule(featureRule(true)).build()
        );
    }

    private static SchemaValidationContext context() {
        return new SchemaValidationContext(SCHEMA);
    }

    private static PatternRecognitionTable minimalTable() {
        return matchRecognize(tbl("sales"))
            .pattern(patternVar("A"))
            .define("A", patternColumn("A", "amount").gt(0))
            .build();
    }
}
