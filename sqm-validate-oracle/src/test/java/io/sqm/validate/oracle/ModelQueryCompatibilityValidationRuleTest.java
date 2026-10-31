package io.sqm.validate.oracle;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.core.Query;
import io.sqm.validate.oracle.rule.ModelQueryCompatibilityValidationRule;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelQueryCompatibilityValidationRuleTest {
    @Test void nestedWithNamesShadowRecursiveMembersWithoutHidingUnshadowedReferences() {
        var anchor = select(lit(1).as("y"), lit(0).as("sales")).build();
        var member = select(col("sales")).from(tbl("forecast")).model(model().dimension("y").measure("sales")
            .rule(cellTarget("sales", 1), lit(1)).build()).build();
        for (var shadow : new boolean[]{true, false}) {
            Query nested = with(cte(shadow ? "forecast" : "other", anchor)).body(member);
            var recursive = with(cte("forecast", anchor.unionAll(nested))).recursive(true)
                .body(select(star()).from(tbl("forecast")).build());
            var context = new SchemaValidationContext(CatalogSchema.allowEverything());
            new ModelQueryCompatibilityValidationRule().validate(recursive, context);
            assertEquals(shadow, context.problems().isEmpty());
            if (!shadow) assertEquals("with.ctes[0].terms[1].model", context.problems().getFirst().clausePath());
        }
    }
}
