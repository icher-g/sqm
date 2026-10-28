package io.sqm.validate.oracle;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.core.*;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.oracle.rule.OracleModelValidationRule;
import io.sqm.validate.schema.SchemaStatementValidator;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class OracleModelValidationTest {
    @Test
    void registersTheRuleAndReportsStructuredProblemsThroughTheNormalPipeline() {
        var dialect = OracleValidationDialect.of();
        var rule = new OracleModelValidationRule();
        assertEquals(SelectQuery.class, rule.nodeType());
        assertTrue(dialect.additionalRules().stream().anyMatch(OracleModelValidationRule.class::isInstance));
        var context = new SchemaValidationContext(CatalogSchema.of());
        rule.validate(select(lit(1)).build(), context);
        assertTrue(context.problems().isEmpty());

        var rules = modelRules().order(RuleOrder.AUTOMATIC).iteration(iterationSpec(col("n")))
            .rule(cellTarget("sales", cellForRange(id("y"), lit("P"), lit(1), lit(2),
                RangeDirection.INCREMENT, lit(1).neg())), lit(1)).build();
        var result = SchemaStatementValidator.of(CatalogSchema.allowEverything(), dialect).validate(query(rules));
        var issues = result.problems().stream().filter(p -> p.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID).toList();
        assertEquals(4, issues.size(), issues.toString());
        assertTrue(issues.stream().anyMatch(p -> p.clausePath().equals("model.main.rules.iteration.limit")));
        assertTrue(issues.stream().anyMatch(p -> p.clausePath().endsWith(".likePattern")));
        assertTrue(issues.stream().anyMatch(p -> p.clausePath().endsWith(".step")));
        assertTrue(issues.stream().allMatch(p -> p.nodeKind() != null && p.message().startsWith("MODEL")));
    }

    @Test
    void validatesLiteralIterationLimitsWithoutThrowing() {
        for (var limit : List.of(lit(1), lit(1).unary("+"), lit(2))) {
            var rules = modelRules().iteration(iterationSpec(limit, lit(1).eq(1)))
                .rule(cellTarget("sales", 1), lit(1)).build();
            assertTrue(problems(rules).isEmpty());
        }
        // Literal invariants are enforced by the core factory; expression-valued
        // limits still need Oracle semantic validation.
        for (var limit : List.of(col("n"), lit(1).neg(), col("n").neg(), lit("one"),
            lit(1).unary("~"), lit(1.5).unary("+"), lit(0).unary("+"), lit(Double.NaN).unary("+"))) {
            assertTrue(problems(modelRules().iteration(iterationSpec(limit))
                .rule(cellTarget("sales", 1), lit(1)).build()).stream()
                .anyMatch(p -> p.clausePath().equals("model.main.rules.iteration.limit")), limit.toString());
        }
    }

    @Test
    void boundsExplicitListsAndTuplesAtTenThousand() {
        var values = IntStream.range(0, 10000).mapToObj(i -> (Object) i).toArray();
        assertTrue(problems(cellForValues("y", values)).isEmpty());
        var tooMany = IntStream.range(0, 10001).mapToObj(i -> (Object) i).toArray();
        assertHas(problems(cellForValues("y", tooMany)), "10000 values");
        assertTrue(problems(cellForValues(List.of(id("p"), id("y")), rows(row("Bike", 2026)))).isEmpty());
        var tuples = IntStream.range(0, 10001).mapToObj(i -> row(i)).toArray(RowExpr[]::new);
        assertHas(problems(cellForValues(List.of(id("y")), rows(tuples))), "10000 values");
    }

    @Test
    void checksRangeStepsTemplatesAndKnownCardinality() {
        for (var range : List.of(
            cellForRange("y", 1, 10000, RangeDirection.INCREMENT, 1),
            cellForRange("y", 10000, 1, RangeDirection.DECREMENT, 1),
            cellForRange("y", 2, 1, RangeDirection.INCREMENT, 1),
            cellForRange(id("p"), lit("P%"), lit(1), lit(2), RangeDirection.INCREMENT, lit(1)),
            cellForRange(id("y"), col("template"), col("start_y"), col("end_y"), RangeDirection.INCREMENT, col("step")),
            cellForRange("y", 1, col("end_y"), RangeDirection.INCREMENT, 1),
            cellForRange("y", 1, 2, RangeDirection.INCREMENT, col("step")))) {
            assertTrue(problems(range).isEmpty(), range.toString());
        }
        assertHas(problems(cellForRange("y", 1, 10001, RangeDirection.INCREMENT, 1)), "10000 values");
        assertHas(problems(cellForRange("y", 1, 2, RangeDirection.INCREMENT, lit(1).neg())), "step must be positive");
        for (var pattern : List.of(lit("P"), lit("P%%"), lit(1))) {
            assertHas(problems(cellForRange(id("p"), pattern, lit(1), lit(2), RangeDirection.INCREMENT, lit(1))),
                "exactly one %");
        }
    }

    @Test
    void checksCombinationsAndContinuesAfterUnknownQueryCounts() {
        var hundred = cellForRange("y", 1, 100, RangeDirection.INCREMENT, 1);
        assertTrue(problems(hundred, hundred).isEmpty());
        assertHas(problems(hundred, cellForRange("z", 1, 101, RangeDirection.INCREMENT, 1)), "10000 combinations");
        assertTrue(problems(cellValue("Bike"), cellForValues("y", 1, 2),
            cellForValues(List.of(id("p"), id("z")), rows(row("A", 1)))).isEmpty());
        var unknown = cellForValues("y", select(lit(1)).build());
        assertTrue(problems(unknown, hundred).isEmpty());
        assertHas(problems(unknown, cellForRange("z", 1, 2, RangeDirection.INCREMENT, lit(1).neg())), "step must be positive");
        assertTrue(problems(cellForRange("y", 2, 1, RangeDirection.INCREMENT, 1), hundred).isEmpty());
    }

    @Test
    void checksQueryWidthThroughWithAndSetQueriesWithoutGuessingStarExpansion() {
        assertTrue(problems(cellForValues("y", select(lit(1)).build())).isEmpty());
        assertTrue(problems(cellForValues(List.of(id("p"), id("y")), select(star()).from(tbl("years")).build())).isEmpty());
        assertHas(problems(cellForValues(List.of(id("p"), id("y")), select(lit(1)).build())), "projection width");
        var withQuery = with(cte("source", select(lit(1)).build())).body(select(lit(1)).build());
        var withIssues = problems(cellForValues(List.of(id("p"), id("y")), withQuery));
        assertHas(withIssues, "projection width");
        assertTrue(withIssues.getFirst().clausePath().endsWith(".values.body"));
        var composite = select(lit(1), lit(2)).build().unionAll(select(lit(1)).build());
        var setIssues = problems(cellForValues(List.of(id("p"), id("y")), composite));
        assertHas(setIssues, "projection width");
        assertTrue(setIssues.getFirst().clausePath().endsWith(".values.terms[1]"));
    }

    @Test
    void validatesEveryRuleAndNestedQueryModelWithoutDoubleReporting() {
        var invalid = modelRules().rule(cellTarget("sales", 1), lit(1))
            .rule(cellTarget("sales", cellForRange("y", 1, 10001, RangeDirection.INCREMENT, 1)), lit(1)).build();
        var issues = problems(invalid);
        assertEquals(1, issues.size());
        assertTrue(issues.getFirst().clausePath().startsWith("model.main.rules.rules[1]"));
        var nested = select(star()).from(tbl(query(invalid)).as("forecast")).build();
        var result = SchemaStatementValidator.of(CatalogSchema.allowEverything(), OracleValidationDialect.of()).validate(nested);
        assertEquals(1, result.problems().stream().filter(p -> p.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID).count());
    }

    private static SelectQuery query(ModelRules rules) {
        return select(lit(1)).model(model().dimension(lit(0), "y").measure(lit(0), "sales").rules(rules).build()).build();
    }

    private static List<ValidationProblem> problems(CellAddress... addresses) {
        return problems(modelRules().rule(cellTarget("sales", addresses), lit(1)).build());
    }

    private static List<ValidationProblem> problems(ModelRules rules) {
        var context = new SchemaValidationContext(CatalogSchema.of());
        new OracleModelValidationRule().validate(query(rules), context);
        return context.problems();
    }

    private static void assertHas(List<ValidationProblem> issues, String message) {
        assertTrue(issues.stream().anyMatch(p -> p.message().contains(message)), issues.toString());
    }
}
