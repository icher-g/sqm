package io.sqm.validate.schema.rule;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogType;
import io.sqm.core.*;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelProjectionShapeTest {
    @Test void modelStarHasAClosedTypedShapeForDmlAndScalarShapeChecks() {
        var query = select(star()).model(model().partition(lit("GB"), "country").dimension(lit(2026), "y")
            .measure(lit(0), "sales").rule(cellTarget("sales", 2026), lit(1)).build()).build();
        var inspector = new DefaultProjectionShapeInspector();
        var context = new SchemaValidationContext(CatalogSchema.of());
        assertEquals(3, inspector.projectionArity(query).orElseThrow());
        assertEquals(3, inspector.expressionProjectionTypes(query, null).orElseThrow().size());
        assertEquals(List.of(col("country"), col("y"), col("sales")), inspector.expressionProjectionExpressions(query).orElseThrow());
        assertEquals(List.of(CatalogType.STRING, CatalogType.INTEGER, CatalogType.INTEGER),
            inspector.expressionProjectionTypes(query, context).orElseThrow().stream().map(type -> type.orElseThrow()).toList());
        assertFalse(inspector.isSingleExpressionProjection(query));
        assertEquals(4, inspector.projectionArity(select(star(), lit(1)).model(query.model()).build()).orElseThrow());
    }
}
