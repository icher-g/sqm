package io.sqm.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FunctionTable Tests")
class FunctionTableTest {

    @Test
    @DisplayName("Create function table without alias")
    void createWithoutAlias() {
        var func = func("generate_series", arg(lit(1)), arg(lit(10)));
        var table = FunctionTable.of(func);

        assertNotNull(table);
        assertEquals(func, table.function());
        assertNull(table.alias());
        assertTrue(table.columnAliases().isEmpty());
    }

    @Test
    @DisplayName("Create function table with alias")
    void createWithAlias() {
        var func = func("unnest", arg(array(lit(1), lit(2), lit(3))));
        var table = FunctionTable.of(func, "t");

        assertNotNull(table);
        assertEquals(func, table.function());
        assertEquals("t", table.alias());
        assertTrue(table.columnAliases().isEmpty());
    }

    @Test
    @DisplayName("Create function table with alias and column aliases")
    void createWithAliasAndColumns() {
        var func = func("json_to_record", arg(lit("{}")));
        var table = FunctionTable.of(func, List.of("id", "name"), "t");

        assertNotNull(table);
        assertEquals(func, table.function());
        assertEquals("t", table.alias());
        assertEquals(2, table.columnAliases().size());
        assertTrue(table.columnAliases().contains("id"));
        assertTrue(table.columnAliases().contains("name"));
    }

    @Test
    @DisplayName("Add alias to function table")
    void addAlias() {
        var func = func("generate_series", arg(lit(1)), arg(lit(10)));
        var table = FunctionTable.of(func).as("series");

        assertEquals("series", table.alias());
        assertEquals(func, table.function());
    }

    @Test
    @DisplayName("Add column aliases as list")
    void addColumnAliasesList() {
        var func = func("generate_series", arg(lit(1)), arg(lit(10)));
        var table = FunctionTable.of(func, "t")
            .columnAliases(List.of("num"));

        assertEquals(1, table.columnAliases().size());
        assertEquals("num", table.columnAliases().getFirst());
    }

    @Test
    @DisplayName("Add column aliases as varargs")
    void addColumnAliasesVarargs() {
        var func = func("json_each", arg(col("data")));
        var table = FunctionTable.of(func, "t")
            .columnAliases("key", "value");

        assertEquals(2, table.columnAliases().size());
        assertEquals("key", table.columnAliases().get(0));
        assertEquals("value", table.columnAliases().get(1));
    }

    @Test
    @DisplayName("Function expression asTable() method")
    void functionExprAsTable() {
        var func = func("generate_series", arg(lit(1)), arg(lit(10)));
        var table = func.asTable();

        assertNotNull(table);
        assertEquals(func, table.function());
        assertNull(table.alias());
    }

    @Test
    @DisplayName("Chaining alias and column aliases")
    void chainingAliasAndColumns() {
        var func = func("generate_series", arg(lit(1)), arg(lit(10)));
        var table = FunctionTable.of(func)
            .as("series")
            .columnAliases("num");

        assertEquals("series", table.alias());
        assertEquals(1, table.columnAliases().size());
        assertEquals("num", table.columnAliases().getFirst());
    }

    @Test
    @DisplayName("FunctionTable is immutable")
    void immutability() {
        var func = func("generate_series", arg(lit(1)), arg(lit(10)));
        var table1 = FunctionTable.of(func);
        var table2 = table1.as("t");
        var table3 = table2.columnAliases("num");

        assertNull(table1.alias());
        assertEquals("t", table2.alias());
        assertEquals("t", table3.alias());
        assertTrue(table1.columnAliases().isEmpty());
        assertTrue(table2.columnAliases().isEmpty());
        assertFalse(table3.columnAliases().isEmpty());
    }

    @Test
    @DisplayName("Accept visitor")
    void acceptVisitor() {
        var func = func("generate_series", arg(lit(1)), arg(lit(10)));
        var table = FunctionTable.of(func, "t");

        var result = table.accept(new TestVisitor());
        assertEquals("FunctionTable", result);
    }

    @Test
    @DisplayName("Column aliases are copied")
    void columnAliasesAreCopied() {
        var func = func("generate_series", arg(lit(1)), arg(lit(10)));
        var columns = new java.util.ArrayList<>(List.of("a", "b"));
        var table = FunctionTable.of(func, columns, "t");

        columns.add("c");
        assertEquals(2, table.columnAliases().size());
    }

    private static class TestVisitor implements io.sqm.core.walk.NodeVisitor<String> {
        @Override
        public String visitFunctionTable(FunctionTable t) {
            return "FunctionTable";
        }

        @Override
        public String visitCaseExpr(CaseExpr c) {
            return null;
        }

        @Override
        public String visitColumnExpr(ColumnExpr c) {
            return null;
        }

        @Override
        public String visitFunctionArgExpr(FunctionExpr.Arg a) {
            return null;
        }

        @Override
        public String visitFunctionExpr(FunctionExpr f) {
            return null;
        }

        @Override
        public String visitAnonymousParamExpr(AnonymousParamExpr p) {
            return null;
        }

        @Override
        public String visitNamedParamExpr(NamedParamExpr p) {
            return null;
        }

        @Override
        public String visitOrdinalParamExpr(OrdinalParamExpr p) {
            return null;
        }

        @Override
        public String visitLiteralExpr(LiteralExpr l) {
            return null;
        }

        @Override
        public String visitQueryExpr(QueryExpr v) {
            return null;
        }

        @Override
        public String visitRowExpr(RowExpr v) {
            return null;
        }

        @Override
        public String visitRowListExpr(RowListExpr v) {
            return null;
        }

        @Override
        public String visitTable(Table t) {
            return null;
        }

        @Override
        public String visitQueryTable(QueryTable t) {
            return null;
        }

        @Override
        public String visitValuesTable(ValuesTable t) {
            return null;
        }

        @Override
        public String visitOnJoin(OnJoin j) {
            return null;
        }

        @Override
        public String visitCrossJoin(CrossJoin j) {
            return null;
        }

        @Override
        public String visitNaturalJoin(NaturalJoin j) {
            return null;
        }

        @Override
        public String visitUsingJoin(UsingJoin j) {
            return null;
        }

        @Override
        public String visitAnyAllPredicate(AnyAllPredicate p) {
            return null;
        }

        @Override
        public String visitBetweenPredicate(BetweenPredicate p) {
            return null;
        }

        @Override
        public String visitComparisonPredicate(ComparisonPredicate p) {
            return null;
        }

        @Override
        public String visitExistsPredicate(ExistsPredicate p) {
            return null;
        }

        @Override
        public String visitIsNullPredicate(IsNullPredicate p) {
            return null;
        }

        @Override
        public String visitLikePredicate(LikePredicate p) {
            return null;
        }

        @Override
        public String visitNotPredicate(NotPredicate p) {
            return null;
        }

        @Override
        public String visitUnaryPredicate(UnaryPredicate p) {
            return null;
        }

        @Override
        public String visitInPredicate(InPredicate p) {
            return null;
        }

        @Override
        public String visitAndPredicate(AndPredicate p) {
            return null;
        }

        @Override
        public String visitOrPredicate(OrPredicate p) {
            return null;
        }

        @Override
        public String visitExprSelectItem(ExprSelectItem i) {
            return null;
        }

        @Override
        public String visitStarSelectItem(StarSelectItem i) {
            return null;
        }

        @Override
        public String visitQualifiedStarSelectItem(QualifiedStarSelectItem i) {
            return null;
        }

        @Override
        public String visitGroupBy(GroupBy g) {
            return null;
        }

        @Override
        public String visitGroupItem(GroupItem i) {
            return null;
        }

        @Override
        public String visitOrderBy(OrderBy o) {
            return null;
        }

        @Override
        public String visitOrderItem(OrderItem i) {
            return null;
        }

        @Override
        public String visitLimitOffset(LimitOffset l) {
            return null;
        }

        @Override
        public String visitCompositeQuery(CompositeQuery q) {
            return null;
        }

        @Override
        public String visitWithQuery(WithQuery q) {
            return null;
        }

        @Override
        public String visitSelectQuery(SelectQuery q) {
            return null;
        }

        @Override
        public String visitWhenThen(WhenThen w) {
            return null;
        }

        @Override
        public String visitCte(CteDef c) {
            return null;
        }

        @Override
        public String visitWindowDef(WindowDef w) {
            return null;
        }

        @Override
        public String visitOverRef(OverSpec.Ref r) {
            return null;
        }

        @Override
        public String visitOverDef(OverSpec.Def d) {
            return null;
        }

        @Override
        public String visitPartitionBy(PartitionBy p) {
            return null;
        }

        @Override
        public String visitFrameSingle(FrameSpec.Single f) {
            return null;
        }

        @Override
        public String visitFrameBetween(FrameSpec.Between f) {
            return null;
        }

        @Override
        public String visitBoundUnboundedPreceding(BoundSpec.UnboundedPreceding b) {
            return null;
        }

        @Override
        public String visitBoundPreceding(BoundSpec.Preceding b) {
            return null;
        }

        @Override
        public String visitBoundCurrentRow(BoundSpec.CurrentRow b) {
            return null;
        }

        @Override
        public String visitBoundFollowing(BoundSpec.Following b) {
            return null;
        }

        @Override
        public String visitBoundUnboundedFollowing(BoundSpec.UnboundedFollowing b) {
            return null;
        }

        @Override
        public String visitAddArithmeticExpr(AddArithmeticExpr expr) {
            return null;
        }

        @Override
        public String visitSubArithmeticExpr(SubArithmeticExpr expr) {
            return null;
        }

        @Override
        public String visitMulArithmeticExpr(MulArithmeticExpr expr) {
            return null;
        }

        @Override
        public String visitDivArithmeticExpr(DivArithmeticExpr expr) {
            return null;
        }

        @Override
        public String visitModArithmeticExpr(ModArithmeticExpr expr) {
            return null;
        }

        @Override
        public String visitNegativeArithmeticExpr(NegativeArithmeticExpr expr) {
            return null;
        }

        @Override
        public String visitDistinctSpec(DistinctSpec spec) {
            return null;
        }

        @Override
        public String visitBinaryOperatorExpr(BinaryOperatorExpr expr) {
            return null;
        }

        @Override
        public String visitUnaryOperatorExpr(UnaryOperatorExpr expr) {
            return null;
        }

        @Override
        public String visitCastExpr(CastExpr expr) {
            return null;
        }

        @Override
        public String visitArrayExpr(ArrayExpr expr) {
            return null;
        }

        @Override
        public String visitTypeName(TypeName typeName) {
            return null;
        }

        @Override
        public String visitRegexPredicate(RegexPredicate p) {
            return null;
        }

        @Override
        public String visitIsDistinctFromPredicate(IsDistinctFromPredicate p) {
            return null;
        }

        @Override
        public String visitLateral(Lateral i) {
            return null;
        }
    }
}
