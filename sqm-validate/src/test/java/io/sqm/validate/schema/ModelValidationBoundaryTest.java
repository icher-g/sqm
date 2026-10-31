package io.sqm.validate.schema;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.core.*;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.rule.ModelClauseFeatureValidationRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelValidationBoundaryTest {
    @Test void rejectsModelByDefaultAndChecksFeatureRuleConfiguration() {
        var query = select(lit(1)).model(model().dimension(lit(1), "y").measure(lit(0), "sales")
            .rule(cellTarget("sales", 1), lit(1)).build()).build();
        var result = SchemaStatementValidator.of(CatalogSchema.of()).validate(query);
        assertTrue(result.problems().stream().anyMatch(p -> p.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
            && p.clausePath().equals("model") && p.message().contains("ANSI")));
        assertThrows(NullPointerException.class, () -> new ModelClauseFeatureValidationRule(null, true));
    }

    @Test void everyModelExpressionRequiresTheCurrentQuerysRuleScope() {
        var cell = cellRef("sales", 1);
        for (var expression : List.of(cell, modelAggregateExpr(func("sum", col("sales")), List.of(cellValue(1))),
            currentDimension(), iterationNumber(), previousModelValueExpr(cell), presenceValue(cell, lit(1), lit(0)),
            cellIsPresent(cell), isAny())) {
            var result = SchemaStatementValidator.of(CatalogSchema.of()).validate(select(expression).build());
            assertTrue(result.problems().stream().anyMatch(p -> p.code() == ValidationProblem.Code.DIALECT_CLAUSE_INVALID
                && p.clausePath().equals("expression.model")), expression.toString());
        }
    }
}
