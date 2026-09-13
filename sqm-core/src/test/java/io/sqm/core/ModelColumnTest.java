package io.sqm.core;

import io.sqm.core.transform.RecursiveNodeTransformer;
import io.sqm.core.walk.RecursiveNodeVisitor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelColumnTest {
    @Test
    void helpersPreserveExpressionsResolvedNamesAndQuoting() {
        var name = id("Sales Year", QuoteStyle.DOUBLE_QUOTE);
        var input = col(id("input"), name);
        var declaration = modelColumn(input);
        assertSame(input, declaration.expression());
        assertEquals(name, declaration.name());
        assertEquals(declaration, modelColumn(input, name));
        assertEquals(modelColumn(col("year"), id("year")), modelColumn("year"));
        assertEquals(modelColumn(col("amount"), "sales"), modelColumn("amount", "sales"));
        assertEquals(lit(0), modelColumn(lit(0), "sales").expression());
        assertThrows(NullPointerException.class, () -> modelColumn((Expression) null, "sales"));
        assertThrows(NullPointerException.class, () -> modelColumn(lit(0), (Identifier) null));
    }

    @Test
    void mainAndReferenceModelsRetainTheirRoleSpecificLists() {
        var country = modelColumn("country");
        var year = modelColumn("sales_year", "year");
        var sales = modelColumn("amount", "sales");
        var main = mainModel().partition(country).dimension(year).measure(sales)
            .rule(cellTarget("sales", 2026), lit(100)).build();
        assertSame(country, main.partitions().getFirst());
        assertSame(year, main.dimensions().getFirst());
        assertSame(sales, main.measures().getFirst());
        assertThrows(UnsupportedOperationException.class, () -> main.partitions().clear());
        assertThrows(UnsupportedOperationException.class, () -> main.dimensions().clear());
        assertThrows(UnsupportedOperationException.class, () -> main.measures().clear());
        var reference = referenceModel("baseline", select(col("sales_year"), col("amount")).from(tbl("history")).build())
            .dimension(year).measure(sales).build();
        assertSame(year, reference.dimensions().getFirst());
        assertSame(sales, reference.measures().getFirst());
        assertEquals(main, MainModel.builder(main).build());
        assertEquals(reference, ReferenceModel.builder(reference).build());
    }

    @Test
    void oneVisitorWalksAllDeclarationRolesAndTheirInputExpressionsInOrder() {
        var main = mainModel().partition("country").dimension("sales_year", "year")
            .measure("amount", "sales").rule(cellTarget("sales", 2026), lit(100)).build();
        var declarations = new ArrayList<ModelColumn>();
        var expressions = new ArrayList<ColumnExpr>();
        main.accept(new RecursiveNodeVisitor<Void>() {
            @Override protected Void defaultResult() { return null; }
            @Override public Void visitModelColumn(ModelColumn column) {
                declarations.add(column);
                return super.visitModelColumn(column);
            }
            @Override public Void visitColumnExpr(ColumnExpr column) {
                expressions.add(column);
                return null;
            }
        });
        assertEquals(List.of(main.partitions().getFirst(), main.dimensions().getFirst(), main.measures().getFirst()), declarations);
        assertEquals(List.of(col("country"), col("sales_year"), col("amount")), expressions);
    }

    @Test
    void changingInputExpressionsRebuildsDeclarationsButPreservesTheirNamesAndRoles() {
        var main = mainModel().partition("country").dimension("sales_year", "year")
            .measure("amount", "sales").rule(cellTarget("sales", 2026), lit(100)).build();
        assertSame(main, new RecursiveNodeTransformer() {}.transform(main));
        var changed = (MainModel) new RecursiveNodeTransformer() {
            @Override public Node visitColumnExpr(ColumnExpr column) {
                return col(id("source"), column.name());
            }
        }.transform(main);
        var original = List.of(main.partitions(), main.dimensions(), main.measures());
        var rewritten = List.of(changed.partitions(), changed.dimensions(), changed.measures());
        for (int i = 0; i < original.size(); i++) {
            var before = original.get(i).getFirst();
            var after = rewritten.get(i).getFirst();
            assertNotSame(before, after);
            assertEquals(before.name(), after.name());
            assertEquals(col(id("source"), ((ColumnExpr) before.expression()).name()), after.expression());
        }
        assertSame(main.rules(), changed.rules());
        assertSame(changed, new RecursiveNodeTransformer() {}.transform(changed));
    }
}
