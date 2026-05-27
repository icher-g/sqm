package io.sqm.core.collect;

import io.sqm.core.*;
import io.sqm.core.walk.RecursiveNodeVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Collector used to collect all table like references in a statement.
 */
public class TableRefsCollector extends RecursiveNodeVisitor<Void> {

    private final List<TableRef> tableRefs = new ArrayList<>();

    /**
     * Creates a new instance of {@link TableRefsCollector}.
     */
    public TableRefsCollector() {

    }

    /**
     * Returns the default result for visitor methods.
     *
     * @return default result
     */
    @Override
    protected Void defaultResult() {
        return null;
    }

    /**
     * Visits a base {@link Table} reference.
     *
     * @param t the table being visited
     * @return a result produced by the visitor
     */
    @Override
    public Void visitTable(Table t) {
        tableRefs.add(t);
        return super.visitTable(t);
    }

    /**
     * Visits a {@link ValuesTable}, representing an inline {@code VALUES} construct.
     *
     * @param t the values table being visited
     * @return a result produced by the visitor
     */
    @Override
    public Void visitValuesTable(ValuesTable t) {
        tableRefs.add(t);
        return super.visitValuesTable(t);
    }

    /**
     * Visits a {@link FunctionTable}, representing function call.
     *
     * @param t the function table being visited
     * @return a result produced by the visitor
     */
    @Override
    public Void visitFunctionTable(FunctionTable t) {
        tableRefs.add(t);
        return super.visitFunctionTable(t);
    }

    /**
     * Visits a {@link JsonTable}.
     *
     * @param t JSON table reference
     * @return a result produced by the visitor
     */
    @Override
    public Void visitJsonTableRef(JsonTable t) {
        tableRefs.add(t);
        return super.visitJsonTableRef(t);
    }

    /**
     * Visits a {@link QueryTable}, representing a derived table
     * or subquery used in the {@code FROM} clause.
     *
     * @param t the query table being visited
     * @return a result produced by the visitor
     */
    @Override
    public Void visitQueryTable(QueryTable t) {
        tableRefs.add(t);
        return super.visitQueryTable(t);
    }

    /**
     * Visits a table-variable reference.
     *
     * @param t the table-variable reference being visited
     * @return a result produced by the visitor
     */
    @Override
    public Void visitVariableTable(VariableTable t) {
        tableRefs.add(t);
        return super.visitVariableTable(t);
    }

    /**
     * Visits a {@link PivotTable}.
     *
     * @param t pivot table reference
     * @return a result produced by the visitor
     */
    @Override
    public Void visitPivotTable(PivotTable t) {
        tableRefs.add(t);
        return super.visitPivotTable(t);
    }

    /**
     * Visits an {@link UnpivotTable}.
     *
     * @param t unpivot table reference
     * @return a result produced by the visitor
     */
    @Override
    public Void visitUnpivotTable(UnpivotTable t) {
        tableRefs.add(t);
        return super.visitUnpivotTable(t);
    }

    /**
     * Visits a {@link Lateral}.
     * <p>
     * A lateral FROM item is evaluated with access to columns of preceding
     * FROM items in the same FROM clause. This enables correlated subqueries
     * and other FROM items whose evaluation depends on earlier sources.
     * <p>
     * Visitors typically use this hook to apply dialect-specific behavior,
     * such as rendering a keyword, validating support, or transforming the
     * wrapped {@link FromItem}.
     *
     * @param i the lateral FROM item
     * @return the visitor result
     */
    @Override
    public Void visitLateral(Lateral i) {
        tableRefs.add(i);
        return super.visitLateral(i);
    }

    /**
     * Returns the collected table references in visitation order.
     *
     * @return collected table references
     */
    public List<TableRef> getTableRefs() {
        return tableRefs;
    }
}
