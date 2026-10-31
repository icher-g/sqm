package io.sqm.validate.schema;

import io.sqm.catalog.model.*;
import io.sqm.core.*;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.rule.ModelClauseFeatureValidationRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class ProjectionAliasValidationTest {
    private static final CatalogSchema SCHEMA = CatalogSchema.of(CatalogTable.of("public", "data",
        CatalogColumn.of("y", CatalogType.INTEGER), CatalogColumn.of("amount", CatalogType.DECIMAL)));
    private static final SchemaStatementValidator VALIDATOR = SchemaStatementValidator.of(SCHEMA,
        SchemaValidationSettings.builder().addRules(List.of(new ModelClauseFeatureValidationRule("native", true))).build());

    @Test void finalOrderByResolvesProjectionAliasesOverStrictTableAndCteSources() {
        valid(select(func("sum", col("amount")).as("total")).from(tbl("data")).orderBy(col("TOTAL")).build());
        valid(with(cte("source", select(col("y"), col("amount")).from(tbl("data")).build()))
            .body(select(func("sum", col("amount")).as("total")).from(tbl("source")).orderBy(col("total")).build()));
        invalid(select(col("amount").as("total")).from(tbl("data")).where(col("total").gt(0)).build(), ValidationProblem.Code.COLUMN_NOT_FOUND);
        invalid(select(col("amount").as("total")).from(tbl("data")).orderBy(col("missing")).build(), ValidationProblem.Code.COLUMN_NOT_FOUND);
        invalid(select(col("amount").as("total")).from(tbl("data").as("d")).orderBy(col("d", "total")).build(), ValidationProblem.Code.COLUMN_NOT_FOUND);
        invalid(select(col("amount").as("total")).from(tbl("data")).where(exists(select(col("total")).from(tbl("data")).build())).build(),
            ValidationProblem.Code.COLUMN_NOT_FOUND);
        invalid(select(col("amount").as("total"), col("y").as("TOTAL")).from(tbl("data")).orderBy(col("total")).build(),
            ValidationProblem.Code.COLUMN_AMBIGUOUS);
    }

    @Test void modelOutputAliasesAreQuoteAwareAndUnavailableInsideRules() {
        var model = model().dimension("y").measure("amount", "sales").rule(cellTarget("sales", 1), lit(1)).build();
        var alias = id("Total", QuoteStyle.DOUBLE_QUOTE);
        valid(select(col("sales").as(alias)).from(tbl("data")).model(model).orderBy(col(alias)).build());
        invalid(select(col("sales").as(alias)).from(tbl("data")).model(model).orderBy(col("total")).build(), ValidationProblem.Code.COLUMN_NOT_FOUND);
        var ordered = model().dimension("y").measure("amount", "sales").rules(modelRules()
            .rule(modelRule(null, cellTarget("sales", 1), orderBy(col("total")), lit(1))).build()).build();
        invalid(select(col("sales").as("total")).from(tbl("data")).model(ordered).orderBy(col("total")).build(), ValidationProblem.Code.COLUMN_NOT_FOUND);
    }

    private static void valid(Statement statement) {
        var result = VALIDATOR.validate(statement);
        assertTrue(result.ok(), () -> result.problems().toString());
    }
    private static void invalid(Statement statement, ValidationProblem.Code code) {
        var result = VALIDATOR.validate(statement);
        assertTrue(result.problems().stream().anyMatch(problem -> problem.code() == code), () -> result.problems().toString());
    }
}
