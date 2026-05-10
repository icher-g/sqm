package io.sqm.parser.ansi;

import io.sqm.core.PivotMeasure;
import io.sqm.core.PivotTable;
import io.sqm.core.PivotValue;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.UnpivotInput;
import io.sqm.core.UnpivotTable;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class PivotTableParserTest {
    private final ParseContext enabled = ParseContext.of(new TestSpecs());
    private final ParseContext ansi = ParseContext.of(new AnsiSpecs());

    @Test
    void rejectsPivotAndUnpivotWhenAnsiCapabilitiesDoNotSupportThem() {
        var pivot = ansi.parse(Query.class, "SELECT * FROM sales PIVOT (sum(amount) FOR quarter IN ('Q1'))");
        var unpivot = ansi.parse(Query.class, "SELECT * FROM sales UNPIVOT (amount FOR quarter IN (q1))");

        assertTrue(pivot.isError());
        assertTrue(Objects.requireNonNull(pivot.errorMessage()).contains("PIVOT is not supported"));
        assertTrue(unpivot.isError());
        assertTrue(Objects.requireNonNull(unpivot.errorMessage()).contains("UNPIVOT is not supported"));
    }

    @Test
    void parsesPivotMeasuresValuesAndAliasesWhenFeatureIsEnabled() {
        var result = enabled.parse(Query.class, """
            SELECT *
            FROM sales
            PIVOT (
                sum(amount) AS total,
                max(discount) max_discount
                FOR quarter IN ('Q1' AS q1, 'Q2' q2)
            ) p
            """);

        assertTrue(result.ok(), result.errorMessage());
        var query = assertInstanceOf(SelectQuery.class, result.value());
        var pivot = assertInstanceOf(PivotTable.class, query.from());
        assertEquals("p", pivot.alias().value());
        assertEquals(2, pivot.measures().size());
        assertEquals("total", pivot.measures().getFirst().alias().value());
        assertEquals("max_discount", pivot.measures().get(1).alias().value());
        assertEquals(2, pivot.values().size());
        assertEquals("q1", pivot.values().getFirst().alias().value());
        assertEquals("q2", pivot.values().get(1).alias().value());
    }

    @Test
    void rejectsPivotXmlContextuallyWithoutKeywordToken() {
        var result = enabled.parse(Query.class, "SELECT * FROM sales PIVOT XML (sum(amount) FOR quarter IN ('Q1'))");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("PIVOT XML"));
    }

    @Test
    void parsesUnpivotNullTreatmentColumnGroupsInputsAndAliasWhenFeatureIsEnabled() {
        var include = enabled.parse(Query.class, """
            SELECT *
            FROM sales
            UNPIVOT INCLUDE NULLS (
                (amount, quantity) FOR quarter IN (
                    (q1_amount, q1_quantity) AS 'Q1',
                    (q2_amount, q2_quantity)
                )
            ) u
            """);
        var exclude = enabled.parse(Query.class, """
            SELECT *
            FROM sales
            UNPIVOT EXCLUDE NULLS (
                amount FOR quarter IN (q1 AS 'Q1')
            )
            """);

        assertTrue(include.ok(), include.errorMessage());
        var includeQuery = assertInstanceOf(SelectQuery.class, include.value());
        var unpivot = assertInstanceOf(UnpivotTable.class, includeQuery.from());
        assertEquals(UnpivotTable.NullTreatment.INCLUDE_NULLS, unpivot.nullTreatment());
        assertEquals(2, unpivot.valueColumns().size());
        assertEquals("u", unpivot.alias().value());
        assertEquals(2, unpivot.inputs().size());
        assertEquals(2, unpivot.inputs().getFirst().sourceColumns().size());
        assertEquals("Q1", unpivot.inputs().getFirst().label().matchExpression().literal(l -> String.valueOf(l.value())).orElse(null));
        assertEquals("q2_amount", unpivot.inputs().get(1).label().matchExpression().literal(l -> String.valueOf(l.value())).orElse(null));

        assertTrue(exclude.ok(), exclude.errorMessage());
        var excludeQuery = assertInstanceOf(SelectQuery.class, exclude.value());
        assertEquals(
            UnpivotTable.NullTreatment.EXCLUDE_NULLS,
            assertInstanceOf(UnpivotTable.class, excludeQuery.from()).nullTreatment()
        );
    }

    @Test
    void parsesPivotAndUnpivotChildNodesDirectly() {
        var measure = enabled.parse(PivotMeasure.class, "sum(amount) AS total");
        var value = enabled.parse(PivotValue.class, "'Q1' AS q1");
        var labeledInput = enabled.parse(UnpivotInput.class, "(q1_amount, q1_quantity) AS 'Q1'");
        var defaultInput = enabled.parse(UnpivotInput.class, "q2_amount");

        assertTrue(measure.ok(), measure.errorMessage());
        assertEquals("total", measure.value().alias().value());
        assertTrue(value.ok(), value.errorMessage());
        assertEquals("q1", value.value().alias().value());
        assertTrue(labeledInput.ok(), labeledInput.errorMessage());
        assertEquals(2, labeledInput.value().sourceColumns().size());
        assertTrue(defaultInput.ok(), defaultInput.errorMessage());
        assertEquals("q2_amount", defaultInput.value().label().matchExpression().literal(l -> String.valueOf(l.value())).orElse(null));
    }

    @Test
    void reportsChildParserErrors() {
        var badMeasure = enabled.parse(Query.class, "SELECT * FROM sales PIVOT (amount + 1 FOR quarter IN ('Q1'))");
        var badValue = enabled.parse(Query.class, "SELECT * FROM sales PIVOT (sum(amount) FOR quarter IN ())");
        var badInputLabel = enabled.parse(UnpivotInput.class, "q1 AS");

        assertTrue(badMeasure.isError());
        assertTrue(badValue.isError());
        assertTrue(badInputLabel.isError());
    }

    @Test
    void exposesParserMetadataAndStandaloneErrors() {
        var pivotParser = new PivotTableParser();
        var unpivotParser = new UnpivotTableParser();

        assertEquals(PivotTable.class, pivotParser.targetType());
        assertEquals(UnpivotTable.class, unpivotParser.targetType());
        assertTrue(pivotParser.match(Cursor.of("PIVOT", enabled.identifierQuoting()), enabled));
        assertTrue(unpivotParser.match(Cursor.of("UNPIVOT", enabled.identifierQuoting()), enabled));
        assertFalse(pivotParser.match(Cursor.of("sales", enabled.identifierQuoting()), enabled));
        assertFalse(unpivotParser.match(Cursor.of("sales", enabled.identifierQuoting()), enabled));
        assertTrue(pivotParser.parse(Cursor.of("PIVOT", enabled.identifierQuoting()), enabled).isError());
        assertTrue(unpivotParser.parse(Cursor.of("UNPIVOT", enabled.identifierQuoting()), enabled).isError());
        assertEquals(TokenType.IDENT, Cursor.of("XML", enabled.identifierQuoting()).peek().type());
    }
}
