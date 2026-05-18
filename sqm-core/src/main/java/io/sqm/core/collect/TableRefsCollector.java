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
     * Returns the default result for visitor methods.
     *
     * @return default result
     */
    @Override
    protected Void defaultResult() {
        return null;
    }

    @Override
    public Void visitTable(Table t) {
        tableRefs.add(t);
        return super.visitTable(t);
    }

    @Override
    public Void visitValuesTable(ValuesTable t) {
        tableRefs.add(t);
        return super.visitValuesTable(t);
    }

    @Override
    public Void visitFunctionTable(FunctionTable t) {
        tableRefs.add(t);
        return super.visitFunctionTable(t);
    }

    @Override
    public Void visitQueryTable(QueryTable t) {
        tableRefs.add(t);
        return super.visitQueryTable(t);
    }

    @Override
    public Void visitVariableTable(VariableTable t) {
        tableRefs.add(t);
        return super.visitVariableTable(t);
    }

    @Override
    public Void visitPivotTable(PivotTable t) {
        tableRefs.add(t);
        return super.visitPivotTable(t);
    }

    @Override
    public Void visitUnpivotTable(UnpivotTable t) {
        tableRefs.add(t);
        return super.visitUnpivotTable(t);
    }

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
