package io.sqm.json;

import io.sqm.core.Expression;
import io.sqm.core.PriorExpr;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class HierarchicalQueryJsonTest {
    @Test
    void roundTripsPriorExpression() throws Exception {
        var mapper = SqmJsonMixins.createPretty();
        var expr = prior(col("id"));

        var json = mapper.writeValueAsString(expr);
        var back = mapper.readValue(json, Expression.class);

        assertTrue(json.contains("\"kind\" : \"prior\""));
        assertInstanceOf(PriorExpr.class, back);
    }

    @Test
    void roundTripsSelectQueryWithHierarchicalClause() throws Exception {
        var mapper = SqmJsonMixins.createPretty();
        var query = select(col("id"))
            .from(tbl("categories"))
            .hierarchical(hierarchy(col("parent_id").isNull(), prior(col("id")).eq(col("parent_id")), true, orderBy(col("name"))))
            .build();

        var json = mapper.writeValueAsString(query);
        var back = mapper.readValue(json, Query.class);

        var select = assertInstanceOf(SelectQuery.class, back);
        assertNotNull(select.hierarchical());
        assertTrue(select.hierarchical().noCycle());
    }
}
