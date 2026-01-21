package io.sqm.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Lateral Tests")
class LateralTest {

    @Test
    @DisplayName("Create lateral with query table")
    void createWithQueryTable() {
        var subquery = select(col("id")).from(tbl("users"));
        var queryTable = tbl(subquery).as("sub");
        var lateral = Lateral.of(queryTable);

        assertNotNull(lateral);
        assertEquals(queryTable, lateral.inner());
    }

    @Test
    @DisplayName("Create lateral with function table")
    void createWithFunctionTable() {
        var func = func("unnest", arg(col("arr")));
        var funcTable = func.asTable().as("t");
        var lateral = Lateral.of(funcTable);

        assertNotNull(lateral);
        assertEquals(funcTable, lateral.inner());
    }

    @Test
    @DisplayName("Create lateral with base table")
    void createWithBaseTable() {
        var table = tbl("users").as("u");
        var lateral = Lateral.of(table);

        assertNotNull(lateral);
        assertEquals(table, lateral.inner());
    }

    @Test
    @DisplayName("lateral() method on TableRef")
    void lateralMethodOnTableRef() {
        var subquery = select(col("*")).from(tbl("orders"));
        var queryTable = tbl(subquery).as("o");
        var lateral = queryTable.lateral();

        assertNotNull(lateral);
        assertInstanceOf(Lateral.class, lateral);
        assertEquals(queryTable, lateral.inner());
    }

    @Test
    @DisplayName("Nested lateral wrapping")
    void nestedLateral() {
        var table = tbl("users").as("u");
        var lateral1 = Lateral.of(table);
        var lateral2 = Lateral.of(lateral1);

        assertEquals(lateral1, lateral2.inner());
        assertEquals(table, lateral1.inner());
    }

    @Test
    @DisplayName("Accept visitor")
    void acceptVisitor() {
        var table = tbl("users").as("u");
        var lateral = Lateral.of(table);

        var result = lateral.accept(new TestVisitor());
        assertEquals("Lateral", result);
    }

    @Test
    @DisplayName("Lateral with VALUES table")
    void lateralWithValuesTable() {
        var values = tbl(rows(row(1, "a"), row(2, "b"))).as("v").columnAliases("id", "name");
        var lateral = Lateral.of(values);

        assertNotNull(lateral);
        assertEquals(values, lateral.inner());
    }

    @Test
    @DisplayName("Null inner throws exception")
    void nullInnerThrowsException() {
        assertThrows(NullPointerException.class, () -> Lateral.of(null));
    }

    private static class TestVisitor implements io.sqm.core.walk.NodeVisitor<String> {
        @Override
        public String visitLateral(Lateral i) {
            return "Lateral";
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
        public String visitFunctionTable(FunctionTable t) {
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
    }
}
