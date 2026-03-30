package io.sqm.transpile.builtin;

import io.sqm.core.*;
import io.sqm.core.transform.RelationTransforms;
import io.sqm.core.transform.RecursiveNodeTransformer;

import java.util.List;

/**
 * Shared transformer that drops all typed statement and table hints from a statement tree.
 */
final class HintDroppingTransformer extends RecursiveNodeTransformer {
    Statement transform(Statement statement) {
        Statement withoutStatementHints = apply(statement);
        return RelationTransforms.rewriteTableRefs(withoutStatementHints, tableRef -> tableRef.<TableRef>matchTableRef()
            .table(table -> table.hints().isEmpty() ? table : table.withHints(List.of()))
            .otherwise(ref -> ref)
        );
    }

    @Override
    public Node visitSelectQuery(SelectQuery query) {
        var transformed = (SelectQuery) super.visitSelectQuery(query);
        if (transformed.hints().isEmpty()) {
            return transformed;
        }
        return SelectQuery.builder(transformed)
            .clearHints()
            .build();
    }

    @Override
    public Node visitUpdateStatement(UpdateStatement statement) {
        var transformed = (UpdateStatement) super.visitUpdateStatement(statement);
        if (transformed.hints().isEmpty()) {
            return transformed;
        }
        return UpdateStatement.builder(transformed)
            .clearHints()
            .build();
    }

    @Override
    public Node visitDeleteStatement(DeleteStatement statement) {
        var transformed = (DeleteStatement) super.visitDeleteStatement(statement);
        if (transformed.hints().isEmpty()) {
            return transformed;
        }
        return DeleteStatement.builder(transformed)
            .clearHints()
            .build();
    }

    @Override
    public Node visitInsertStatement(InsertStatement statement) {
        var transformed = (InsertStatement) super.visitInsertStatement(statement);
        if (transformed.hints().isEmpty()) {
            return transformed;
        }
        return InsertStatement.builder(transformed)
            .clearHints()
            .build();
    }

    @Override
    public Node visitMergeStatement(MergeStatement statement) {
        var transformed = (MergeStatement) super.visitMergeStatement(statement);
        if (transformed.hints().isEmpty()) {
            return transformed;
        }
        return MergeStatement.builder(transformed)
            .clearHints()
            .build();
    }

}
