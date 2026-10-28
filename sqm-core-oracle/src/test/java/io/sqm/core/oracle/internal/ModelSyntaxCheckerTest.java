package io.sqm.core.oracle.internal;

import io.sqm.core.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelSyntaxCheckerTest {
    @Test
    void acceptsOrdinarySymbolicConditionsAndExplicitCompoundWildcards() {
        for (Predicate predicate : List.of(
            col("y").eq(2026), col("y").between(2025, 2027), col("y").between(2025, 2027).negated(true),
            col("y").in(2025, 2026), col("y").isNull(), isAny(), col("y").isAny(),
            col("y").gt(2025).and(col("y").lt(2027)),
            col("y").eq(2025).or(col("y").eq(2026)), col("y").isNull().not(),
            col("product").like("Bike%"),
            col("product").like(lit("Bike%")).escape(lit("!")))) {
            assertDoesNotThrow(() -> ModelSyntaxChecker.selector(predicate));
        }
    }

    @Test
    void rejectsQueryAndBooleanFallbacksInSelectorConditions() {
        for (Predicate predicate : List.of(
            col("y").in(expr(select(col("y")).from(tbl("years")).build())),
            isAny().and(col("y").eq(2025)),
            lit(1).isAny(),
            col("t", "y").isAny(),
            col("product").ilike("Bike%"),
            unary(col("flag")),
            col("y").between(lit(1), lit(2)).symmetric(true))){
            assertThrows(IllegalArgumentException.class, () -> ModelSyntaxChecker.selector(predicate));
        }
        assertThrows(IllegalArgumentException.class, () -> ModelSyntaxChecker.scalar(col("y").eq(1)));
        assertThrows(IllegalArgumentException.class, () -> ModelSyntaxChecker.scalar(row(1, 2)));
        assertThrows(IllegalArgumentException.class, () -> ModelSyntaxChecker.scalar(rows(row(1, 2))));
        assertThrows(IllegalArgumentException.class, () -> ModelSyntaxChecker.scalar(expr(select(lit(1)).build())));
        assertDoesNotThrow(() -> ModelSyntaxChecker.scalar(func("ABS", lit(-1))));
    }

    @Test
    void leavesValueRestrictionsAndQuerySemanticsToValidation() {
        var tooMany = IntStream.range(0, 10001).mapToObj(i -> (Object) i).toArray();
        assertDoesNotThrow(() -> ModelSyntaxChecker.values(cellForValues("y", tooMany)));
        assertDoesNotThrow(() -> ModelSyntaxChecker.values(cellForValues(List.of(id("p"), id("y")),
            select(lit(1)).build())));
        assertDoesNotThrow(() -> ModelSyntaxChecker.values(cellForValues(List.of(id("p"), id("y")),
            rows(row("Bike", 2026)))));
        assertDoesNotThrow(() -> ModelSyntaxChecker.range(cellForRange("y", 1, 10001, RangeDirection.INCREMENT, 1)));
        assertDoesNotThrow(() -> ModelSyntaxChecker.range(cellForRange("y", 1, 2, RangeDirection.INCREMENT, lit(1).neg())));
        assertDoesNotThrow(() -> ModelSyntaxChecker.range(
            cellForRange(id("p"), lit("no-wildcard"), lit(1), lit(2), RangeDirection.INCREMENT, lit(1))));
        assertDoesNotThrow(() -> ModelSyntaxChecker.range(
            cellForRange(id("p"), null, col("start"), col("end"), RangeDirection.INCREMENT, col("step"))));
        assertDoesNotThrow(() -> ModelSyntaxChecker.iteration(iterationSpec(col("n"))));
        assertDoesNotThrow(() -> ModelSyntaxChecker.iteration(iterationSpec(lit(1).neg())));
        assertDoesNotThrow(() -> ModelSyntaxChecker.iteration(iterationSpec(lit(2), col("y").gt(1))));
    }

    @Test
    void stillRejectsUnsupportedExpressionShapesAtBothSqlBoundaries() {
        assertThrows(IllegalArgumentException.class, () -> ModelSyntaxChecker.values(
            cellForValues("y", expr(select(lit(1)).build()), 2)));
        assertThrows(IllegalArgumentException.class, () -> ModelSyntaxChecker.range(
            cellForRange("y", row(1, 2), 2, RangeDirection.INCREMENT, 1)));
        assertThrows(IllegalArgumentException.class, () -> ModelSyntaxChecker.iteration(
            iterationSpec(lit(1), col("y").in(expr(select(lit(1)).build())))));
        assertDoesNotThrow(() -> ModelSyntaxChecker.orderBy(null));
        assertDoesNotThrow(() -> ModelSyntaxChecker.orderBy(orderBy(col("y"))));
        assertThrows(IllegalArgumentException.class, () -> ModelSyntaxChecker.orderBy(orderBy(order(1))));
    }
}
