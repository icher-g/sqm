package io.sqm.core.walk;

import io.sqm.core.*;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class RecordingVisitor extends RecursiveNodeVisitor<Void> {

    private final Set<String> seen = new LinkedHashSet<>();

    public Set<String> seen() {
        return seen;
    }

    private void mark(Object o) {
        if (o != null) {
            var i = Arrays.stream(o.getClass().getInterfaces()).findFirst();
            var t = i.orElse(o.getClass());
            seen.add(t.getSimpleName());
        }
    }

    @Override
    protected Void defaultResult() {
        return null;
    }

    @Override
    public Void visitCaseExpr(CaseExpr c) {
        mark(c);
        return super.visitCaseExpr(c);
    }

    @Override
    public Void visitColumnExpr(ColumnExpr c) {
        mark(c);
        return super.visitColumnExpr(c);
    }

    @Override
    public Void visitFunctionArgExpr(FunctionExpr.Arg a) {
        mark(a);
        return super.visitFunctionArgExpr(a);
    }

    @Override
    public Void visitFunctionExpr(FunctionExpr f) {
        mark(f);
        return super.visitFunctionExpr(f);
    }

    @Override
    public Void visitLiteralExpr(LiteralExpr l) {
        mark(l);
        return super.visitLiteralExpr(l);
    }

    @Override
    public Void visitQueryExpr(QueryExpr v) {
        mark(v);
        return super.visitQueryExpr(v);
    }

    @Override
    public Void visitRowExpr(RowExpr v) {
        mark(v);
        return super.visitRowExpr(v);
    }

    @Override
    public Void visitRowListExpr(RowListExpr v) {
        mark(v);
        return super.visitRowListExpr(v);
    }

    @Override
    public Void visitTable(Table t) {
        mark(t);
        return super.visitTable(t);
    }

    @Override
    public Void visitQueryTable(QueryTable t) {
        mark(t);
        return super.visitQueryTable(t);
    }

    @Override
    public Void visitValuesTable(ValuesTable t) {
        mark(t);
        return super.visitValuesTable(t);
    }

    @Override
    public Void visitOnJoin(OnJoin j) {
        mark(j);
        return super.visitOnJoin(j);
    }

    @Override
    public Void visitCrossJoin(CrossJoin j) {
        mark(j);
        return super.visitCrossJoin(j);
    }

    @Override
    public Void visitNaturalJoin(NaturalJoin j) {
        mark(j);
        return super.visitNaturalJoin(j);
    }

    @Override
    public Void visitUsingJoin(UsingJoin j) {
        mark(j);
        return super.visitUsingJoin(j);
    }

    @Override
    public Void visitAnyAllPredicate(AnyAllPredicate p) {
        mark(p);
        return super.visitAnyAllPredicate(p);
    }

    @Override
    public Void visitBetweenPredicate(BetweenPredicate p) {
        mark(p);
        return super.visitBetweenPredicate(p);
    }

    @Override
    public Void visitComparisonPredicate(ComparisonPredicate p) {
        mark(p);
        return super.visitComparisonPredicate(p);
    }

    @Override
    public Void visitExistsPredicate(ExistsPredicate p) {
        mark(p);
        return super.visitExistsPredicate(p);
    }

    @Override
    public Void visitIsNullPredicate(IsNullPredicate p) {
        mark(p);
        return super.visitIsNullPredicate(p);
    }

    @Override
    public Void visitLikePredicate(LikePredicate p) {
        mark(p);
        return super.visitLikePredicate(p);
    }

    @Override
    public Void visitNotPredicate(NotPredicate p) {
        mark(p);
        return super.visitNotPredicate(p);
    }

    @Override
    public Void visitUnaryPredicate(UnaryPredicate p) {
        mark(p);
        return super.visitUnaryPredicate(p);
    }

    @Override
    public Void visitInPredicate(InPredicate p) {
        mark(p);
        return super.visitInPredicate(p);
    }

    @Override
    public Void visitAndPredicate(AndPredicate p) {
        mark(p);
        return super.visitAndPredicate(p);
    }

    @Override
    public Void visitOrPredicate(OrPredicate p) {
        mark(p);
        return super.visitOrPredicate(p);
    }

    @Override
    public Void visitExprSelectItem(ExprSelectItem i) {
        mark(i);
        return super.visitExprSelectItem(i);
    }

    @Override
    public Void visitStarSelectItem(StarSelectItem i) {
        mark(i);
        return super.visitStarSelectItem(i);
    }

    @Override
    public Void visitQualifiedStarSelectItem(QualifiedStarSelectItem i) {
        mark(i);
        return super.visitQualifiedStarSelectItem(i);
    }

    @Override
    public Void visitGroupBy(GroupBy g) {
        mark(g);
        return super.visitGroupBy(g);
    }

    @Override
    public Void visitSimpleGroupItem(GroupItem.SimpleGroupItem i) {
        mark(i);
        return super.visitSimpleGroupItem(i);
    }

    @Override
    public Void visitOrderBy(OrderBy o) {
        mark(o);
        return super.visitOrderBy(o);
    }

    @Override
    public Void visitOrderItem(OrderItem i) {
        mark(i);
        return super.visitOrderItem(i);
    }

    @Override
    public Void visitLimitOffset(LimitOffset l) {
        mark(l);
        return super.visitLimitOffset(l);
    }

    @Override
    public Void visitCompositeQuery(CompositeQuery q) {
        mark(q);
        return super.visitCompositeQuery(q);
    }

    @Override
    public Void visitWithQuery(WithQuery q) {
        mark(q);
        return super.visitWithQuery(q);
    }

    @Override
    public Void visitSelectQuery(SelectQuery q) {
        mark(q);
        return super.visitSelectQuery(q);
    }

    @Override
    public Void visitWhenThen(WhenThen w) {
        mark(w);
        return super.visitWhenThen(w);
    }

    @Override
    public Void visitCte(CteDef c) {
        mark(c);
        return super.visitCte(c);
    }

    @Override
    public Void visitPartitionBy(PartitionBy p) {
        mark(p);
        return super.visitPartitionBy(p);
    }

    @Override
    public Void visitBoundCurrentRow(BoundSpec.CurrentRow b) {
        mark(b);
        return super.visitBoundCurrentRow(b);
    }

    @Override
    public Void visitBoundFollowing(BoundSpec.Following b) {
        mark(b);
        return super.visitBoundFollowing(b);
    }

    @Override
    public Void visitBoundPreceding(BoundSpec.Preceding b) {
        mark(b);
        return super.visitBoundPreceding(b);
    }

    @Override
    public Void visitBoundUnboundedFollowing(BoundSpec.UnboundedFollowing b) {
        mark(b);
        return super.visitBoundUnboundedFollowing(b);
    }

    @Override
    public Void visitFrameBetween(FrameSpec.Between f) {
        mark(f);
        return super.visitFrameBetween(f);
    }

    @Override
    public Void visitFrameSingle(FrameSpec.Single f) {
        mark(f);
        return super.visitFrameSingle(f);
    }

    @Override
    public Void visitBoundUnboundedPreceding(BoundSpec.UnboundedPreceding b) {
        mark(b);
        return super.visitBoundUnboundedPreceding(b);
    }

    @Override
    public Void visitOverDef(OverSpec.Def d) {
        mark(d);
        return super.visitOverDef(d);
    }

    @Override
    public Void visitWindowDef(WindowDef w) {
        mark(w);
        return super.visitWindowDef(w);
    }

    @Override
    public Void visitOverRef(OverSpec.Ref r) {
        mark(r);
        return super.visitOverRef(r);
    }
}
