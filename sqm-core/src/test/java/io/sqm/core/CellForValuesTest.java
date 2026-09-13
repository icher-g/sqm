package io.sqm.core;

import io.sqm.core.transform.RecursiveNodeTransformer;
import io.sqm.core.walk.RecursiveNodeVisitor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class CellForValuesTest {
    @Test
    void dslReusesScalarTupleAndQuerySourcesWithoutWrappingThemAsLiterals() {
        var scalar = row(2026, lit(2027));
        assertSame(scalar, cellForValues("year", scalar).values());
        assertEquals(scalar, cellForValues("year", 2026, lit(2027)).values());
        var tuple = rows(row("Bike", 2026), row("Car", 2027));
        assertSame(tuple, cellForValues(List.of(id("product"), id("year")), tuple).values());
        var query = select(col("forecast_year")).from(tbl("forecasts")).build();
        var source = expr(query);
        assertSame(source, cellForValues("year", source).values());
        assertSame(query, ((QueryExpr) cellForValues("year", query).values()).subquery());
        assertEquals(cellForValues("year", query), cellForValues(List.of(id("year")), source));
        var tupleQuery = select(col("product"), col("forecast_year")).from(tbl("forecasts")).build();
        assertEquals(expr(tupleQuery), cellForValues(List.of(id("product"), id("year")), tupleQuery).values());
        assertEquals(rows(row(2026)), cellForValues("year", rows(row(2026))).values());
        assertThrows(NullPointerException.class, () -> cellForValues("year", expr((Query) null)));
    }

    @Test
    void tupleListHelperConvertsScalarsAndRetainsExpressions() {
        var expression = col("forecast_year");
        var tuple = new ArrayList<Object>();
        tuple.add("Bike");
        tuple.add(expression);
        tuple.add(null);
        var source = rows(List.of(tuple));
        assertEquals(rows(row("Bike", expression, (Object) null)), source);
        assertSame(expression, source.rows().getFirst().items().get(1));
        assertSame(source, cellForValues(List.of(id("product"), id("year"), id("region")), source).values());
    }

    @Test
    void recursiveVisitorsWalkTheSharedSourceNodesAndTheirChildren() {
        for (var generator : generators()) {
            var visited = new ArrayList<Node>();
            generator.accept(new RecursiveNodeVisitor<Void>() {
                @Override protected Void defaultResult() { return null; }
                @Override public Void visitRowExpr(RowExpr node) {
                    visited.add(node);
                    return super.visitRowExpr(node);
                }
                @Override public Void visitRowListExpr(RowListExpr node) {
                    visited.add(node);
                    return super.visitRowListExpr(node);
                }
                @Override public Void visitQueryExpr(QueryExpr node) {
                    visited.add(node);
                    return super.visitQueryExpr(node);
                }
                @Override public Void visitLiteralExpr(LiteralExpr node) {
                    visited.add(node);
                    return null;
                }
            });
            assertSame(generator.values(), visited.getFirst());
            assertTrue(visited.contains(lit(2026)));
            assertTrue(visited.contains(lit(2027)));
        }
    }

    @Test
    void transformationsRebuildTheSharedSourceAndRetainUnchangedChildren() {
        var unchanged = new RecursiveNodeTransformer() {};
        var rewrite = new RecursiveNodeTransformer() {
            @Override public Node visitLiteralExpr(LiteralExpr node) {
                return node.equals(lit(2026)) ? lit(2030) : node;
            }
        };
        for (var generator : generators()) {
            assertSame(generator, unchanged.transform(generator));
            var updated = (CellFor.Values) rewrite.transform(generator);
            assertNotSame(generator, updated);
            assertNotSame(generator.values(), updated.values());
            assertEquals(generator.dimensions(), updated.dimensions());
            assertSame(updated, unchanged.transform(updated));
            var originalLiterals = literals(generator);
            var updatedLiterals = literals(updated);
            assertEquals(List.of(lit(2026), lit(2027)), originalLiterals);
            assertEquals(List.of(lit(2030), lit(2027)), updatedLiterals);
            assertSame(originalLiterals.getLast(), updatedLiterals.getLast());
        }
    }

    private static List<CellFor.Values> generators() {
        return List.of(
            cellForValues("year", 2026, 2027),
            cellForValues(List.of(id("from_year"), id("to_year")), rows(row(2026, 2027))),
            cellForValues("year", select(lit(2026)).where(col("year").eq(2027)).build()));
    }

    private static List<LiteralExpr> literals(Node node) {
        var result = new ArrayList<LiteralExpr>();
        node.accept(new RecursiveNodeVisitor<Void>() {
            @Override protected Void defaultResult() { return null; }
            @Override public Void visitLiteralExpr(LiteralExpr literal) {
                result.add(literal);
                return null;
            }
        });
        return result;
    }
}
