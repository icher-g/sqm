package io.sqm.validate.postgresql;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.SchemaStatementValidator;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatternRecognitionValidationTest {
    @Test
    void rejectsPatternRecognition() {
        var table = matchRecognize(tbl("sales"))
            .pattern(patternVar("A"))
            .define("A", patternColumn("A", "amount").gt(0))
            .build();

        var result = SchemaStatementValidator.of(CatalogSchema.of(), PostgresValidationDialect.of())
            .validate(select(star()).from(table).build());

        assertTrue(result.problems().stream().anyMatch(problem ->
            problem.code() == ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED
                && "from.matchRecognize".equals(problem.clausePath())));
    }
}
