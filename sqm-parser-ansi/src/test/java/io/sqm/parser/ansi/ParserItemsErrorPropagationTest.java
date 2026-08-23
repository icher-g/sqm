package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserItemsErrorPropagationTest {
    private final ParseContext ctx = ParseContext.of(new TestSpecs());

    @Test
    void propagatesItemErrorsFromDelimitedParsers() {
        assertTrue(ctx.parse(OrderBy.class, "ORDER BY ,").isError());
        assertTrue(ctx.parse(PartitionBy.class, "PARTITION BY ,").isError());
        assertTrue(ctx.parse(RowExpr.class, "(,)").isError());
        assertTrue(ctx.parse(RowListExpr.class, "(,)").isError());
        assertTrue(ctx.parse(ValuesTable.class, "(VALUES ,)").isError());
        assertTrue(ctx.parse(WithQuery.class, "WITH , SELECT 1").isError());
        assertTrue(ctx.parse(TypeName.class, "varchar(,)").isError());
    }

    @Test
    void valuesTableSupportsSingleAndMultipleRows() {
        var single = ctx.parse(Query.class, "SELECT * FROM (VALUES (1)) AS single_row(value)");
        var multiple = ctx.parse(Query.class, "SELECT * FROM (VALUES (1), (2)) AS multiple_rows(value)");

        assertTrue(single.ok(), single.errorMessage());
        assertTrue(multiple.ok(), multiple.errorMessage());
        var singleValues = assertInstanceOf(ValuesTable.class,
            assertInstanceOf(SelectQuery.class, single.value()).from());
        var multipleValues = assertInstanceOf(ValuesTable.class,
            assertInstanceOf(SelectQuery.class, multiple.value()).from());
        assertInstanceOf(RowExpr.class, singleValues.values());
        assertEquals(2, assertInstanceOf(RowListExpr.class, multipleValues.values()).rows().size());
    }
}
