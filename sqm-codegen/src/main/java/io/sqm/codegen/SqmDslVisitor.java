package io.sqm.codegen;

import io.sqm.core.*;
import io.sqm.core.walk.RecursiveNodeVisitor;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

final class SqmDslVisitor extends RecursiveNodeVisitor<Void> {

    private final CodeBuilder out = CodeBuilder.of();

    private static IllegalStateException unsupported(String label, Object value) {
        return new IllegalStateException("Unsupported " + label + ": " + value.getClass().getName());
    }

    private void appendExcludeOrNull(OverSpec.Exclude exclude) {
        switch (exclude) {
            case CURRENT_ROW -> out.append("excludeCurrentRow()");
            case GROUP -> out.append("excludeGroup()");
            case TIES -> out.append("excludeTies()");
            case NO_OTHERS -> out.append("excludeNoOthers()");
            case null -> out.append("null");
        }
    }

    private void appendLikeMethod(LikeMode mode, boolean negated) {
        switch (mode) {
            case LIKE -> out.append(negated ? "notLike" : "like");
            case ILIKE -> out.append(negated ? "notIlike" : "ilike");
            case SIMILAR_TO -> out.append(negated ? "notSimilarTo" : "similarTo");
        }
    }

    private void appendLiteralValue(Object value) {
        switch (value) {
            case null -> out.append("null");
            case String stringValue -> out.quote(stringValue);
            case Character charValue -> out.quote(String.valueOf(charValue), '\'');
            case Long ignored -> out.append(value + "L");
            case Float ignored -> out.append(value + "F");
            case Short ignored -> out.append("(short)" + value);
            case Byte ignored -> out.append("(byte)" + value);
            case Boolean ignored -> out.append(String.valueOf(value));
            case Integer ignored -> out.append(String.valueOf(value));
            case Double ignored -> out.append(String.valueOf(value));
            default -> throw new IllegalStateException("Unsupported literal value type: " + Objects.requireNonNull(value).getClass().getName());
        }
    }

    private void appendIdentifier(Identifier value) {
        if (value.quoteStyle() == QuoteStyle.NONE) {
            out.append("id(").quote(value.value()).append(")");
        }
        else {
            out.append("id(").quote(value.value()).append(", QuoteStyle." + value.quoteStyle().name() + ")");
        }
    }

    private void appendIdentifierList(List<Identifier> values) {
        out.append("List.of(").comma(values, this::appendIdentifier).append(")");
    }

    private void appendQualifiedName(QualifiedName value) {
        out.append("qualify(").comma(value.parts(), this::appendIdentifier).append(")");
    }

    private void appendLockMode(LockMode mode) {
        switch (mode) {
            case UPDATE -> out.append("update()");
            case NO_KEY_UPDATE -> out.append("noKeyUpdate()");
            case SHARE -> out.append("share()");
            case KEY_SHARE -> out.append("keyShare()");
        }
    }

    private void appendLockTargets(List<LockTarget> targets) {
        if (targets == null || targets.isEmpty()) {
            out.append("List.of()");
            return;
        }
        out.append("ofTables(");
        out.comma(targets, p -> out.quote(p.identifier().value()));
        out.append(")");
    }

    private void appendResultBuilderCall(ResultClause clause) {
        if (clause.into() == null) {
            out.append(".result(").comma(clause.items(), this::appendNode).append(")");
        }
        else {
            out.append(".result(");
            appendNode(clause.into());
            out.append(", ").comma(clause.items(), this::appendNode).append(")");
        }
    }

    private void appendHintArgsSuffix(List<HintArg> args) {
        if (!args.isEmpty()) {
            out.append(", ").comma(args, this::appendNode);
        }
    }

    private void appendDistinctTail(DistinctSpec distinctSpec) {
        if (distinctSpec.items() == null || distinctSpec.items().isEmpty()) {
            out.append(".distinct(distinct())");
        }
        else {
            out.append(".distinct(").comma(distinctSpec.items(), this::appendNode).append(")");
        }
    }

    private void appendNode(Node node) {
        node.accept(this);
    }

    private void appendFunctionExpr(FunctionExpr f) {
        if (f.name() == null) {
            appendUnnamedFunction(f);
        }
        else {
            var functionName = f.name().parts().getLast().value().toLowerCase(Locale.ROOT);
            switch (functionName) {
                case "len" -> appendUnaryKnownFunction("len", f);
                case "datalength" -> appendUnaryKnownFunction("dataLength", f);
                case "getdate" -> {
                    if (f.args() == null || f.args().isEmpty()) out.append("getDate()");
                    else appendUnnamedFunction(f);
                }
                case "dateadd" -> appendDateKnownFunction("dateAdd", f);
                case "datediff" -> appendDateKnownFunction("dateDiff", f);
                case "isnull" -> appendBinaryKnownFunction("isNullFn", f);
                case "string_agg" -> appendBinaryKnownFunction("stringAgg", f);
                default -> appendUnnamedFunction(f);
            }
        }
    }

    private void appendUnnamedFunction(FunctionExpr f) {
        out.append("func(").quote(String.join(".", f.name().values()));
        if (!f.args().isEmpty()) {
            out.append(", ").comma(f.args(), this::appendNode);
        }
        out.append(")");
    }

    private void appendUnaryKnownFunction(String helperName, FunctionExpr f) {
        if (f.args() == null || f.args().size() != 1) {
            appendUnnamedFunction(f);
            return;
        }

        out.append(helperName).append("(");
        appendExprArg(f.args().getFirst());
        out.append(")");
    }

    private void appendBinaryKnownFunction(String helperName, FunctionExpr f) {
        if (f.args() == null || f.args().size() != 2) {
            appendUnnamedFunction(f);
            return;
        }

        out.append(helperName).append("(");
        appendExprArg(f.args().getFirst());
        out.append(", ");
        appendExprArg(f.args().get(1));
        out.append(")");
    }

    private void appendDateKnownFunction(String helperName, FunctionExpr f) {
        if (f.args() == null || f.args().size() != 3) {
            appendUnnamedFunction(f);
            return;
        }

        var firstArg = f.args().getFirst();
        if (!(firstArg instanceof FunctionExpr.Arg.ExprArg exprArg)
            || !(exprArg.expr() instanceof LiteralExpr literal)
            || !(literal.value() instanceof String datePart)) {
            appendUnnamedFunction(f);
            return;
        }

        out.append(helperName).append("(").quote(datePart).append(", ");
        appendExprArg(f.args().get(1));
        out.append(", ");
        appendExprArg(f.args().get(2));
        out.append(")");
    }

    private void appendExprArg(FunctionExpr.Arg arg) {
        if (arg instanceof FunctionExpr.Arg.ExprArg exprArg) {
            appendNode(exprArg.expr());
        }
        else {
            appendNode(arg);
        }
    }

    private void appendOrderByOrNull(OrderBy orderBy) {
        if (orderBy == null) {
            out.append("null");
        }
        else {
            appendNode(orderBy);
        }
    }

    public String emit(Statement statement) {
        appendNode(statement);
        var result = out.toString();
        out.reset();
        return result;
    }

    @Override
    protected Void defaultResult() {
        return null;
    }

    @Override
    public Void visitSelectQuery(SelectQuery q) {
        out.append("select(").nl();
        out.in();
        out.comma(q.items(), this::appendNode, true);
        out.out();
        out.nl().append(")");

        if (q.from() != null) {
            out.nl().append(".from(");
            appendNode(q.from());
            out.append(")");
        }
        if (q.joins() != null && !q.joins().isEmpty()) {
            out.nl().append(".join(").nl();
            out.in();
            out.comma(q.joins(), this::appendNode, true);
            out.out();
            out.nl().append(")");
        }
        if (q.where() != null) {
            out.nl().append(".where(");
            try (var ignore = new CodeScope(out, false)) {
                appendNode(q.where());
            }
            out.append(")");
        }
        if (q.groupBy() != null && q.groupBy().items() != null && !q.groupBy().items().isEmpty()) {
            out.nl().append(".groupBy(");
            try (var ignore = new CodeScope(out, false)) {
                out.comma(q.groupBy().items(), this::appendNode, true);
            }
            out.append(")");
        }
        if (q.having() != null) {
            out.nl().append(".having(");
            appendNode(q.having());
            out.append(")");
        }
        if (q.windows() != null && !q.windows().isEmpty()) {
            out.nl().append(".window(");
            try (var ignore = new CodeScope(out, false)) {
                out.comma(q.windows(), this::appendNode, true);
            }
            out.append(")");
        }
        if (q.orderBy() != null && q.orderBy().items() != null && !q.orderBy().items().isEmpty()) {
            out.nl().append(".orderBy(");
            try (var ignore = new CodeScope(out, false)) {
                out.comma(q.orderBy().items(), this::appendNode, true);
            }
            out.append(")");
        }
        if (q.distinct() != null) {
            out.nl();
            appendDistinctTail(q.distinct());
        }
        if (q.topSpec() != null) {
            out.nl();
            this.visitTopSpec(q.topSpec());
        }
        if (q.limitOffset() != null) {
            out.nl();
            this.visitLimitOffset(q.limitOffset());
        }
        if (q.lockFor() != null) {
            out.nl().append(".lockFor(");
            this.visitLockingClause(q.lockFor());
            out.append(")");
        }
        for (var hint : q.hints()) {
            out.nl().append(".");
            this.visitStatementHint(hint);
        }
        out.nl().append(".build()");
        return defaultResult();
    }

    @Override
    public Void visitWithQuery(WithQuery q) {
        out.append("with(")
            .comma(q.ctes(), this::appendNode)
            .append(")");
        if (q.recursive()) {
            out.nl().append(".recursive(true)");
        }
        if (q.body() != null) {
            out.nl().append(".body(");
            out.in();
            appendNode(q.body());
            out.out();
            out.append(")");
        }
        return defaultResult();
    }

    @Override
    public Void visitCompositeQuery(CompositeQuery q) {
        out.append("compose(").nl()
            .in()
            .append("List.of(").nl()
            .in()
            .comma(q.terms(), this::appendNode, true)
            .out()
            .nl().append(")");
        if (!q.ops().isEmpty()) {
            out.append(",").nl()
                .append("List.of(").nl()
                .in()
                .comma(q.ops(), op -> out.append("SetOperator." + op.name()))
                .out().nl()
                .append(")");
        }
        out.out().nl().append(")");
        return defaultResult();
    }

    @Override
    public Void visitCte(CteDef cte) {
        out.append("cte(").quote(cte.name().value()).append(", ");
        if (cte.body() == null) out.append("null");
        else appendNode(cte.body());
        out.append(")");

        if (cte.columnAliases() != null && !cte.columnAliases().isEmpty()) {
            out.append(".columnAliases(")
                .comma(cte.columnAliases(), i -> out.quote(i.value()))
                .append(")");
        }

        if (cte.materialization() != null && cte.materialization() != CteDef.Materialization.DEFAULT) {
            out.append(".materialization(CteDef.Materialization.")
                .append(cte.materialization().name())
                .append(")");
        }
        return defaultResult();
    }

    @Override
    public Void visitInsertStatement(InsertStatement statement) {
        out.append("insert(");
        appendNode(statement.table());
        out.append(")");
        for (var hint : statement.hints()) {
            out.nl().append(".");
            this.visitStatementHint(hint);
        }
        if (statement.insertMode() == InsertStatement.InsertMode.IGNORE) {
            out.nl().append(".ignore()");
        }
        if (statement.insertMode() == InsertStatement.InsertMode.REPLACE) {
            out.nl().append(".replace()");
        }
        if (!statement.columns().isEmpty()) {
            out.nl().append(".columns(")
                .comma(statement.columns(), this::appendIdentifier)
                .append(")");
        }
        if (statement.source() instanceof Query query) {
            out.nl().append(".query(");
            appendNode(query);
            out.append(")");
        }
        else {
            out.nl().append(".values(");
            appendNode(statement.source());
            out.append(")");
        }
        switch (statement.onConflictAction()) {
            case NONE -> {
            }
            case DO_NOTHING -> {
                if (statement.conflictTarget().isEmpty()) {
                    out.nl().append(".onConflictDoNothing()");
                }
                else {
                    out.nl().append(".onConflictDoNothing(")
                        .comma(statement.conflictTarget(), this::appendIdentifier)
                        .append(")");
                }
            }
            case DO_UPDATE -> {
                if (statement.conflictTarget().isEmpty() && statement.conflictUpdateWhere() == null) {
                    out.nl().append(".onConflictDoUpdate(")
                        .comma(statement.conflictUpdateAssignments(), this::appendNode)
                        .append(")");
                }
                else {
                    out.nl().append(".onConflictDoUpdate(");
                    appendIdentifierList(statement.conflictTarget());
                    out.append(", List.of(")
                        .comma(statement.conflictUpdateAssignments(), this::appendNode)
                        .append("), ");

                    if (statement.conflictUpdateWhere() == null) out.append("null");
                    else appendNode(statement.conflictUpdateWhere());

                    out.append(")");
                }
            }
        }
        if (statement.result() != null) {
            out.nl();
            appendResultBuilderCall(statement.result());
        }
        out.nl().append(".build()");
        return defaultResult();
    }

    @Override
    public Void visitUpdateStatement(UpdateStatement statement) {
        out.append("update(");
        appendNode(statement.table());
        out.append(")");
        for (var hint : statement.hints()) {
            out.nl().append(".");
            this.visitStatementHint(hint);
        }
        if (!statement.joins().isEmpty()) {
            out.nl().append(".joins(")
                .comma(statement.joins(), this::appendNode)
                .append(")");
        }
        for (var assignment : statement.assignments()) {
            out.nl().append(".set(");
            if (assignment.column().parts().size() > 2) {
                appendQualifiedName(assignment.column());
                out.append(", ");
                appendNode(assignment.value());
            }
            else {
                for (var part : assignment.column().parts()) {
                    if (part.quoted()) {
                        appendIdentifier(part);
                        out.append(", ");
                    }
                    else {
                        out.quote(part.value()).append(", ");
                    }
                }
                appendNode(assignment.value());
            }
            out.append(")");
        }
        if (!statement.from().isEmpty()) {
            out.nl().append(".from(")
                .comma(statement.from(), this::appendNode)
                .append(")");
        }
        if (statement.where() != null) {
            out.nl().append(".where(");
            appendNode(statement.where());
            out.append(")");
        }
        if (statement.result() != null) {
            out.nl();
            appendResultBuilderCall(statement.result());
        }
        out.nl().append(".build()");
        return defaultResult();
    }

    @Override
    public Void visitDeleteStatement(DeleteStatement statement) {
        out.append("delete(");
        appendNode(statement.table());
        out.append(")");
        for (var hint : statement.hints()) {
            out.nl().append(".");
            this.visitStatementHint(hint);
        }
        if (!statement.using().isEmpty()) {
            out.nl().append(".using(")
                .comma(statement.using(), this::appendNode)
                .append(")");
        }
        if (!statement.joins().isEmpty()) {
            out.nl().append(".joins(")
                .comma(statement.joins(), this::appendNode)
                .append(")");
        }
        if (statement.where() != null) {
            out.nl().append(".where(");
            appendNode(statement.where());
            out.append(")");
        }
        if (statement.result() != null) {
            out.nl();
            appendResultBuilderCall(statement.result());
        }
        out.nl().append(".build()");
        return defaultResult();
    }

    @Override
    public Void visitMergeStatement(MergeStatement statement) {
        out.append("merge(");
        appendNode(statement.target());
        out.append(")");
        for (var hint : statement.hints()) {
            out.nl().append(".");
            this.visitStatementHint(hint);
        }
        out.nl().append(".source(");
        appendNode(statement.source());
        out.append(")");
        out.nl().append(".on(");
        appendNode(statement.on());
        out.append(")");
        if (statement.topSpec() != null) {
            out.nl();
            this.visitTopSpec(statement.topSpec());
        }
        for (var clause : statement.clauses()) {
            out.nl();
            this.visitMergeClause(clause);
        }
        if (statement.result() != null) {
            out.nl();
            appendResultBuilderCall(statement.result());
        }
        out.nl().append(".build()");
        return defaultResult();
    }

    @Override
    public Void visitMergeClause(MergeClause clause) {
        if (clause.matchType() == MergeClause.MatchType.MATCHED && clause.action() instanceof MergeUpdateAction updateAction) {
            out.append(".whenMatchedUpdate(");
            if (clause.condition() != null) {
                appendNode(clause.condition());
                out.append(", ");
            }
            out.comma(updateAction.assignments(), this::appendNode);
            out.append(")");
            return defaultResult();
        }
        if (clause.matchType() == MergeClause.MatchType.MATCHED && clause.action() instanceof MergeDeleteAction) {
            out.append(".whenMatchedDelete(");
            if (clause.condition() != null) {
                appendNode(clause.condition());
            }
            out.append(")");
            return defaultResult();
        }
        if (clause.matchType() == MergeClause.MatchType.MATCHED && clause.action() instanceof MergeDoNothingAction) {
            out.append(".whenMatchedDoNothing(");
            if (clause.condition() != null) {
                appendNode(clause.condition());
            }
            out.append(")");
            return defaultResult();
        }
        if (clause.matchType() == MergeClause.MatchType.NOT_MATCHED && clause.action() instanceof MergeInsertAction insertAction) {
            out.append(".whenNotMatchedInsert(");
            if (clause.condition() != null) {
                appendNode(clause.condition());
                out.append(", ");
            }
            if (!insertAction.columns().isEmpty()) {
                appendIdentifierList(insertAction.columns());
                out.append(", ");
            }
            appendNode(insertAction.values());
            out.append(")");
            return defaultResult();
        }
        if (clause.matchType() == MergeClause.MatchType.NOT_MATCHED && clause.action() instanceof MergeDoNothingAction) {
            out.append(".whenNotMatchedDoNothing(");
            if (clause.condition() != null) {
                appendNode(clause.condition());
            }
            out.append(")");
            return defaultResult();
        }
        if (clause.matchType() == MergeClause.MatchType.NOT_MATCHED_BY_SOURCE && clause.action() instanceof MergeUpdateAction updateAction) {
            out.append(".whenNotMatchedBySourceUpdate(");
            if (clause.condition() != null) {
                appendNode(clause.condition());
                out.append(", ");
            }
            out.comma(updateAction.assignments(), this::appendNode);
            out.append(")");
            return defaultResult();
        }
        if (clause.matchType() == MergeClause.MatchType.NOT_MATCHED_BY_SOURCE && clause.action() instanceof MergeDeleteAction) {
            out.append(".whenNotMatchedBySourceDelete(");
            if (clause.condition() != null) {
                appendNode(clause.condition());
            }
            out.append(")");
            return defaultResult();
        }
        if (clause.matchType() == MergeClause.MatchType.NOT_MATCHED_BY_SOURCE && clause.action() instanceof MergeDoNothingAction) {
            out.append(".whenNotMatchedBySourceDoNothing(");
            if (clause.condition() != null) {
                appendNode(clause.condition());
            }
            out.append(")");
            return defaultResult();
        }
        throw unsupported("merge clause", clause);
    }

    @Override
    public Void visitMergeUpdateAction(MergeUpdateAction action) {
        // handled directly in visitMergeClause().
        return super.visitMergeUpdateAction(action);
    }

    @Override
    public Void visitMergeInsertAction(MergeInsertAction action) {
        // handled directly in visitMergeClause().
        return super.visitMergeInsertAction(action);
    }

    @Override
    public Void visitMergeDoNothingAction(MergeDoNothingAction action) {
        // handled directly in visitMergeClause().
        return super.visitMergeDoNothingAction(action);
    }

    @Override
    public Void visitMergeDeleteAction(MergeDeleteAction action) {
        // handled directly in visitMergeClause().
        return super.visitMergeDeleteAction(action);
    }

    @Override
    public Void visitAssignment(Assignment assignment) {
        out.append("set(");
        if (assignment.column().parts().size() == 1) {
            appendIdentifier(assignment.column().parts().getFirst());
        }
        else {
            appendQualifiedName(assignment.column());
        }
        out.append(", ");
        appendNode(assignment.value());
        out.append(")");
        return defaultResult();
    }


    @Override
    public Void visitExprSelectItem(ExprSelectItem i) {
        var lineNumber = out.currentLineNumber();
        appendNode(i.expr());
        if (i.alias() != null) {
            try (var ignore = new CodeScope(out, lineNumber == out.currentLineNumber())) {
                out.append(".as(").quote(i.alias().value()).append(")");
            }
        }
        return defaultResult();
    }

    @Override
    public Void visitStarSelectItem(StarSelectItem i) {
        out.append("star()");
        return defaultResult();
    }

    @Override
    public Void visitQualifiedStarSelectItem(QualifiedStarSelectItem i) {
        out.append("star(").quote(i.qualifier().value()).append(")");
        return defaultResult();
    }

    @Override
    public Void visitTable(Table t) {
        if (t.schema() == null) {
            out.append("tbl(").quote(t.name().value()).append(")");
        }
        else {
            out.append("tbl(").quote(t.schema().value()).append(", ").quote(t.name().value()).append(")");
        }

        if (t.alias() != null) {
            out.append(".as(").quote(t.alias().value()).append(")");
        }
        if (t.inheritance() == Table.Inheritance.ONLY) {
            out.append(".only()");
        }
        if (t.inheritance() == Table.Inheritance.INCLUDE_DESCENDANTS) {
            out.append(".includingDescendants()");
        }
        for (var hint : t.hints()) {
            out.append(".");
            this.visitTableHint(hint);
        }
        return defaultResult();
    }

    @Override
    public Void visitStatementHint(StatementHint hint) {
        out.append("hint(").quote(hint.name().value());
        appendHintArgsSuffix(hint.args());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitTableHint(TableHint hint) {
        switch (hint.name().value()) {
            case "NOLOCK" -> out.append("withNoLock()");
            case "UPDLOCK" -> out.append("withUpdLock()");
            case "HOLDLOCK" -> out.append("withHoldLock()");
            default -> {
                out.append("hint(").quote(hint.name().value());
                appendHintArgsSuffix(hint.args());
                out.append(")");
            }
        }
        return defaultResult();
    }

    @Override
    public Void visitExpressionHintArg(ExpressionHintArg arg) {
        if (arg.value() instanceof LiteralExpr literalExpr && !(literalExpr.value() instanceof String)) {
            appendLiteralValue(literalExpr.value());
        }
        else {
            appendNode(arg.value());
        }
        return defaultResult();
    }

    @Override
    public Void visitIdentifierHintArg(IdentifierHintArg arg) {
        out.quote(arg.value().value());
        return defaultResult();
    }

    @Override
    public Void visitQualifiedNameHintArg(QualifiedNameHintArg arg) {
        out.append("qualify(").comma(arg.value().parts(), this::appendIdentifier).append(")");
        return defaultResult();
    }

    @Override
    public Void visitQueryTable(QueryTable t) {
        out.append("tbl(");
        try (var ignore = new CodeScope(out, false)) {
            appendNode(t.query());
        }
        out.append(")");
        if (t.alias() != null) {
            out.append(".as(").quote(t.alias().value()).append(")");
        }
        if (t.columnAliases() != null && !t.columnAliases().isEmpty()) {
            out.append(".columnAliases(").comma(t.columnAliases(), i -> out.quote(i.value())).append(")");
        }
        return defaultResult();
    }

    @Override
    public Void visitLateral(Lateral l) {
        appendNode(l.inner());
        out.append(".lateral()");
        return defaultResult();
    }

    @Override
    public Void visitVariableTableRef(VariableTableRef t) {
        out.append("tableVar(").quote(t.name().value()).append(")");
        return defaultResult();
    }

    @Override
    public Void visitOnJoin(OnJoin j) {
        String fn = switch (j.kind()) {
            case INNER -> "inner";
            case LEFT -> "left";
            case RIGHT -> "right";
            case FULL -> "full";
            case STRAIGHT -> "straight";
        };
        out.append(fn).append("(");
        appendNode(j.right());
        out.append(")");
        if (j.on() != null) {
            out.append(".on(");
            appendNode(j.on());
            out.append(")");
        }
        return defaultResult();
    }

    @Override
    public Void visitCrossJoin(CrossJoin j) {
        out.append("cross(");
        appendNode(j.right());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitNaturalJoin(NaturalJoin j) {
        out.append("natural(");
        appendNode(j.right());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitUsingJoin(UsingJoin j) {
        String fn = switch (j.kind()) {
            case INNER -> "inner";
            case LEFT -> "left";
            case RIGHT -> "right";
            case FULL -> "full";
            case STRAIGHT -> "straight";
        };
        out.append(fn).append("(");
        appendNode(j.right());
        out.append(")");
        out.append(".using(").comma(j.usingColumns(), i -> out.quote(i.value())).append(")");
        return defaultResult();
    }

    @Override
    public Void visitSimpleGroupItem(GroupItem.SimpleGroupItem i) {
        if (i.ordinal() == null) {
            out.append("group(");
            appendNode(i.expr());
            out.append(")");
        }
        else {
            out.append("group(").append(i.ordinal().toString()).append(")");
        }
        return defaultResult();
    }

    @Override
    public Void visitGroupingSet(GroupItem.GroupingSet i) {
        out.append("groupingSet(").comma(i.items(), this::appendNode).append(")");
        return defaultResult();
    }

    @Override
    public Void visitGroupingSets(GroupItem.GroupingSets i) {
        out.append("groupingSets(").comma(i.sets(), this::appendNode).append(")");
        return defaultResult();
    }

    @Override
    public Void visitRollup(GroupItem.Rollup i) {
        out.append("rollup(").comma(i.items(), this::appendNode).append(")");
        return defaultResult();
    }

    @Override
    public Void visitCube(GroupItem.Cube i) {
        out.append("cube(").comma(i.items(), this::appendNode).append(")");
        return defaultResult();
    }

    @Override
    public Void visitOrderItem(OrderItem i) {
        if (i.expr() != null) {
            out.append("order(");
            appendNode(i.expr());
            out.append(")");
        }
        else {
            if (i.ordinal() != null) {
                out.append("order(").append(i.ordinal().toString()).append(")");
            }
            else {
                throw new IllegalStateException("Order item must have expression or ordinal");
            }
        }
        if (i.direction() != null) {
            out.append(i.direction().name().equals("ASC") ? ".asc()" : ".desc()");
        }
        if (i.nulls() != null) {
            out.append(switch (i.nulls()) {
                case FIRST -> ".nullsFirst()";
                case LAST -> ".nullsLast()";
                case DEFAULT -> ".nullsDefault()";
            });
        }
        if (i.collate() != null) {
            out.append(".collate(").quote(String.join(".", i.collate().values())).append(")");
        }
        if (i.usingOperator() != null) {
            out.append(".using(").quote(i.usingOperator()).append(")");
        }
        return defaultResult();
    }

    @Override
    public Void visitComparisonPredicate(ComparisonPredicate p) {
        String fn = switch (p.operator()) {
            case EQ -> "eq";
            case NE -> "ne";
            case NULL_SAFE_EQ -> "nullSafeEq";
            case LT -> "lt";
            case LTE -> "lte";
            case GT -> "gt";
            case GTE -> "gte";
        };
        appendNode(p.lhs());
        out.append(".").append(fn).append("(");
        appendNode(p.rhs());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitAndPredicate(AndPredicate p) {
        appendNode(p.lhs());
        out.append(".and(");
        out.nl();
        appendNode(p.rhs());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitOrPredicate(OrPredicate p) {
        appendNode(p.lhs());
        out.append(".or(");
        out.nl();
        appendNode(p.rhs());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitNotPredicate(NotPredicate p) {
        appendNode(p.inner());
        out.append(".not()");
        return defaultResult();
    }

    @Override
    public Void visitUnaryPredicate(UnaryPredicate p) {
        out.append("unary(");
        appendNode(p.expr());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitIsNullPredicate(IsNullPredicate p) {
        if (p.negated()) {
            appendNode(p.expr());
            out.append(".isNotNull()");
        }
        else {
            appendNode(p.expr());
            out.append(".isNull()");
        }
        return defaultResult();
    }

    @Override
    public Void visitInPredicate(InPredicate p) {
        if (p.negated()) {
            appendNode(p.lhs());
            out.append(".notIn(");
            appendNode(p.rhs());
            out.append(")");
        }
        else {
            appendNode(p.lhs());
            out.append(".in(");
            appendNode(p.rhs());
            out.append(")");
        }
        return defaultResult();
    }

    @Override
    public Void visitBetweenPredicate(BetweenPredicate p) {
        appendNode(p.value());
        out.append(".between(");
        appendNode(p.lower());
        out.append(", ");
        appendNode(p.upper());
        out.append(")");
        if (p.symmetric()) {
            out.append(".symmetric(true)");
        }
        if (p.negated()) {
            out.append(".negated(true)");
        }
        return defaultResult();
    }

    @Override
    public Void visitLikePredicate(LikePredicate p) {
        appendNode(p.value());
        out.append(".");
        appendLikeMethod(p.mode(), p.negated());
        out.append("(");
        appendNode(p.pattern());
        out.append(")");
        if (p.escape() != null) {
            out.append(".escape(");
            appendNode(p.escape());
            out.append(")");
        }
        return defaultResult();
    }

    @Override
    public Void visitExistsPredicate(ExistsPredicate p) {
        if (p.negated()) out.append("notExists(");
        else out.append("exists(");
        try (var ignore = new CodeScope(out, false)) {
            appendNode(p.subquery());
        }
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitAnyAllPredicate(AnyAllPredicate p) {
        var method = p.quantifier() == Quantifier.ANY ? ".any(" : ".all(";
        appendNode(p.lhs());
        out.append(method).append("ComparisonOperator.").append(p.operator().name());
        out.append(", ");
        appendNode(p.subquery());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitArrayExpr(ArrayExpr expr) {
        out.append("array(").comma(expr.elements(), this::appendNode).append(")");
        return defaultResult();
    }

    @Override
    public Void visitArraySliceExpr(ArraySliceExpr expr) {
        appendNode(expr.base());
        out.append(".slice(");
        if (expr.from().isEmpty()) out.append("null");
        else appendNode(expr.from().get());
        out.append(", ");
        if (expr.to().isEmpty()) out.append("null");
        else appendNode(expr.to().get());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitArraySubscriptExpr(ArraySubscriptExpr expr) {
        appendNode(expr.base());
        out.append(".at(");
        appendNode(expr.index());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitColumnExpr(ColumnExpr c) {
        if (c.tableAlias() == null) {
            out.append("col(").quote(c.name().value()).append(")");
        }
        else {
            out.append("col(").quote(c.tableAlias().value()).append(", ").quote(c.name().value()).append(")");
        }
        return defaultResult();
    }

    @Override
    public Void visitOutputColumnExpr(OutputColumnExpr c) {
        var helper = c.source() == OutputRowSource.INSERTED ? "inserted" : "deleted";
        out.append(helper).append("(");
        appendIdentifier(c.column());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitAtTimeZoneExpr(AtTimeZoneExpr expr) {
        appendNode(expr.timestamp());
        out.append(".atTimeZone(");
        appendNode(expr.timezone());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitExprResultItem(ExprResultItem item) {
        appendNode(item.expr());
        if (item.alias() != null) {
            out.append(".as(");
            appendIdentifier(item.alias());
            out.append(")");
        }
        return defaultResult();
    }

    @Override
    public Void visitStarResultItem(StarResultItem item) {
        out.append("star()");
        return defaultResult();
    }

    @Override
    public Void visitQualifiedStarResultItem(QualifiedStarResultItem item) {
        out.append("star(");
        appendIdentifier(item.qualifier());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitOutputStarResultItem(OutputStarResultItem item) {
        out.append(item.source() == OutputRowSource.INSERTED ? "insertedAll()" : "deletedAll()");
        return defaultResult();
    }

    @Override
    public Void visitResultInto(ResultInto into) {
        out.append("resultInto(");
        appendNode(into.target());
        if (!into.columns().isEmpty()) {
            out.append(", ").comma(into.columns(), this::appendIdentifier);
        }
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitResultClause(ResultClause clause) {
        out.append("result(");
        if (clause.into() != null) {
            appendNode(clause.into());
            out.append(", ");
        }
        out.comma(clause.items(), this::appendNode).append(")");
        return defaultResult();
    }

    @Override
    public Void visitLiteralExpr(LiteralExpr l) {
        out.append("lit(");
        appendLiteralValue(l.value());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitDateLiteralExpr(DateLiteralExpr l) {
        out.append("date(").quote(l.value()).append(")");
        return defaultResult();
    }

    @Override
    public Void visitBitStringLiteralExpr(BitStringLiteralExpr l) {
        out.append("bit(").quote(l.value()).append(")");
        return defaultResult();
    }

    @Override
    public Void visitHexStringLiteralExpr(HexStringLiteralExpr l) {
        out.append("hex(").quote(l.value()).append(")");
        return defaultResult();
    }

    @Override
    public Void visitIntervalLiteralExpr(IntervalLiteralExpr l) {
        out.append("interval(").quote(l.value());
        if (l.qualifier().isPresent()) {
            out.append(", ").quote(l.qualifier().get());
        }
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitTimestampLiteralExpr(TimestampLiteralExpr l) {
        out.append("timestamp(").quote(l.value()).append(", TimeZoneSpec.").append(l.timeZoneSpec().name()).append(")");
        return defaultResult();
    }

    @Override
    public Void visitTimeLiteralExpr(TimeLiteralExpr l) {
        out.append("time(").quote(l.value()).append(", TimeZoneSpec.").append(l.timeZoneSpec().name()).append(")");
        return defaultResult();
    }

    @Override
    public Void visitDollarStringLiteralExpr(DollarStringLiteralExpr l) {
        out.append("dollar(").quote(l.tag()).append(", ").quote(l.value()).append(")");
        return defaultResult();
    }

    @Override
    public Void visitEscapeStringLiteralExpr(EscapeStringLiteralExpr l) {
        out.append("escape(").quote(l.value()).append(")");
        return defaultResult();
    }

    @Override
    public Void visitCastExpr(CastExpr expr) {
        appendNode(expr.expr());
        out.append(".cast(");
        appendNode(expr.type());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitBinaryOperatorExpr(BinaryOperatorExpr expr) {
        appendNode(expr.left());
        out.append(".op(");
        if (expr.operator().operatorKeywordSyntax()) {
            if (expr.operator().qualified()) {
                out.append("op(qualify(").comma(expr.operator().schemaName().values(), v -> out.quote(v)).append("), ").quote(expr.operator().symbol()).append(")");
            }
            else {
                out.append("op(").quote(expr.operator().symbol()).append(")");
            }
        }
        else {
            out.quote(expr.operator().symbol());
        }
        out.append(", ");
        appendNode(expr.right());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitTypeName(TypeName typeName) {
        if (typeName.keyword().isPresent()) {
            out.append("type(TypeKeyword.").append(typeName.keyword().get().name()).append(")");
        }
        else {
            out.append("type(").append("qualify(").comma(typeName.qualifiedName().parts(), this::appendIdentifier).append(")");
            if (!typeName.modifiers().isEmpty()) {
                out.append(", List.of(").comma(typeName.modifiers(), this::appendNode).append(")");
                if (typeName.timeZoneSpec() != TimeZoneSpec.NONE && typeName.arrayDims() == 0) {
                    out.append(", 0");
                }
            }
            if (typeName.arrayDims() > 0) {
                out.append(", ");
                appendLiteralValue(typeName.arrayDims());
            }
            if (typeName.timeZoneSpec() != TimeZoneSpec.NONE) {
                out.append(", TimeZoneSpec.").append(typeName.timeZoneSpec().name());
            }
            out.append(")");
        }
        return defaultResult();
    }

    @Override
    public Void visitCollateExpr(CollateExpr expr) {
        var collation = String.join(".", expr.collation().parts().stream().map(p -> p.value()).toList());
        appendNode(expr.expr());
        out.append(".collate(").quote(collation).append(")");
        return defaultResult();
    }

    @Override
    public Void visitConcatExpr(ConcatExpr expr) {
        out.append("concat(").comma(expr.args(), this::appendNode).append(")");
        return defaultResult();
    }

    @Override
    public Void visitGroupBy(GroupBy g) {
        out.append("groupBy(").comma(g.items(), this::appendNode).append(")");
        return defaultResult();
    }

    @Override
    public Void visitOrderBy(OrderBy o) {
        out.append("orderBy(").comma(o.items(), this::appendNode).append(")");
        return defaultResult();
    }

    @Override
    public Void visitFunctionTable(FunctionTable t) {
        out.append("tbl(");
        try (var ignore = new CodeScope(out, false)) {
            appendNode(t.function());
        }
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitValuesTable(ValuesTable t) {
        out.append("tbl(");
        try (var ignore = new CodeScope(out, false)) {
            appendNode(t.values());
        }
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitIsDistinctFromPredicate(IsDistinctFromPredicate p) {
        appendNode(p.lhs());
        if (p.negated()) out.append(".isNotDistinctFrom(");
        else out.append(".isDistinctFrom(");
        appendNode(p.rhs());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitUnaryOperatorExpr(UnaryOperatorExpr expr) {
        appendNode(expr.expr());
        out.append(".unary(").quote(expr.operator().symbol()).append(")");
        return defaultResult();
    }

    @Override
    public Void visitNegativeArithmeticExpr(NegativeArithmeticExpr expr) {
        appendNode(expr.expr());
        out.append(".neg()");
        return defaultResult();
    }

    @Override
    public Void visitDivArithmeticExpr(DivArithmeticExpr expr) {
        appendNode(expr.lhs());
        out.append(".div(");
        appendNode(expr.rhs());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitModArithmeticExpr(ModArithmeticExpr expr) {
        appendNode(expr.lhs());
        out.append(".mod(");
        appendNode(expr.rhs());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitMulArithmeticExpr(MulArithmeticExpr expr) {
        appendNode(expr.lhs());
        out.append(".mul(");
        appendNode(expr.rhs());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitPowerArithmeticExpr(PowerArithmeticExpr expr) {
        appendNode(expr.lhs());
        out.append(".pow(");
        appendNode(expr.rhs());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitSubArithmeticExpr(SubArithmeticExpr expr) {
        appendNode(expr.lhs());
        out.append(".sub(");
        appendNode(expr.rhs());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitRegexPredicate(RegexPredicate p) {
        appendNode(p.value());
        out.append(".regex(RegexMode.").append(p.mode().name()).append(", ");
        appendNode(p.pattern());
        out.append(", ").append(String.valueOf(p.negated())).append(")");
        return defaultResult();
    }

    @Override
    public Void visitAnonymousParamExpr(AnonymousParamExpr p) {
        out.append("param()");
        return defaultResult();
    }

    @Override
    public Void visitNamedParamExpr(NamedParamExpr p) {
        out.append("param(").quote(p.name()).append(")");
        return defaultResult();
    }

    @Override
    public Void visitOrdinalParamExpr(OrdinalParamExpr p) {
        out.append("param(").append(String.valueOf(p.index())).append(")");
        return defaultResult();
    }

    @Override
    public Void visitRowExpr(RowExpr v) {
        out.append("row(").comma(v.items(), this::appendNode).append(")");
        return defaultResult();
    }

    @Override
    public Void visitRowListExpr(RowListExpr v) {
        out.append("rows(").comma(v.rows(), this::appendNode).append(")");
        return defaultResult();
    }

    @Override
    public Void visitQueryExpr(QueryExpr v) {
        out.append("expr(");
        appendNode(v.subquery());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitFunctionArgExpr(FunctionExpr.Arg a) {
        if (a instanceof FunctionExpr.Arg.ExprArg exprArg) {
            out.append("arg(");
            appendNode(exprArg.expr());
            out.append(")");
            return defaultResult();
        }
        if (a instanceof FunctionExpr.Arg.StarArg) {
            out.append("starArg()");
            return defaultResult();
        }
        throw unsupported("function argument", a);
    }

    @Override
    public Void visitFunctionExpr(FunctionExpr f) {
        appendFunctionExpr(f);
        out.in();
        if (Boolean.TRUE.equals(f.distinctArg())) {
            out.nl().append(".distinct()");
        }
        if (f.withinGroup() != null) {
            out.nl().append(".withinGroup(");
            try (var ignore = new CodeScope(out, false)) {
                out.comma(f.withinGroup().items(), this::appendNode, true);
            }
            out.append(")");
        }
        if (f.filter() != null) {
            out.nl().append(".filter(");
            try (var ignore = new CodeScope(out, false)) {
                appendNode(f.filter());
            }
            out.append(")");
        }
        if (f.over() != null) {
            out.nl().append(".over(");
            if (f.over() instanceof OverSpec.Def def
                && def.baseWindow() == null
                && def.partitionBy() == null
                && def.orderBy() == null
                && def.frame() == null
                && def.exclude() == null) {
                out.append("over()");
            }
            else {
                try (var ignore = new CodeScope(out, f.over() instanceof OverSpec.Ref)) {
                    appendNode(f.over());
                }
            }
            out.append(")");
        }
        out.out();
        return defaultResult();
    }

    @Override
    public Void visitWindowDef(WindowDef w) {
        out.append("window(").quote(w.name().value()).append(", ");
        appendNode(w.spec());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitOverRef(OverSpec.Ref r) {
        out.quote(r.windowName().value());
        return defaultResult();
    }

    @Override
    public Void visitOverDef(OverSpec.Def d) {
        if (d.baseWindow() != null) {
            if (d.frame() != null && d.exclude() != null) {
                out.quote(d.baseWindow().value()).append(", ");
                appendOrderByOrNull(d.orderBy());
                out.append(", ");
                appendNode(d.frame());
                out.append(", ");
                appendExcludeOrNull(d.exclude());
                return defaultResult();
            }
            if (d.frame() != null && d.orderBy() != null) {
                out.quote(d.baseWindow().value()).append(", ");
                appendNode(d.orderBy());
                out.append(", ");
                appendNode(d.frame());
                return defaultResult();
            }
            if (d.frame() != null) {
                out.quote(d.baseWindow().value()).append(", ");
                appendNode(d.frame());
                return defaultResult();
            }
            if (d.orderBy() != null) {
                out.quote(d.baseWindow().value()).append(", ");
                appendNode(d.orderBy());
                return defaultResult();
            }
            out.quote(d.baseWindow().value());
            return defaultResult();
        }
        if (d.partitionBy() != null) {
            if (d.frame() != null && d.exclude() != null) {
                appendNode(d.partitionBy());
                out.append(", ");
                appendOrderByOrNull(d.orderBy());
                out.append(", ");
                appendNode(d.frame());
                out.append(", ");
                appendExcludeOrNull(d.exclude());
                return defaultResult();
            }
            if (d.frame() != null && d.orderBy() != null) {
                appendNode(d.partitionBy());
                out.append(", ");
                appendNode(d.orderBy());
                out.append(", ");
                appendNode(d.frame());
                return defaultResult();
            }
            if (d.frame() != null) {
                appendNode(d.partitionBy());
                out.append(", ");
                appendNode(d.frame());
                return defaultResult();
            }
            if (d.orderBy() != null) {
                appendNode(d.partitionBy());
                out.append(", ");
                appendNode(d.orderBy());
                return defaultResult();
            }
            appendNode(d.partitionBy());
            return defaultResult();
        }
        if (d.frame() != null && d.exclude() != null) {
            appendOrderByOrNull(d.orderBy());
            out.append(", ");
            appendNode(d.frame());
            out.append(", ");
            appendExcludeOrNull(d.exclude());
            return defaultResult();
        }
        if (d.frame() != null && d.orderBy() != null) {
            appendNode(d.orderBy());
            out.append(", ");
            appendNode(d.frame());
            return defaultResult();
        }
        if (d.frame() != null) {
            appendNode(d.frame());
            return defaultResult();
        }
        if (d.orderBy() != null) {
            appendNode(d.orderBy());
            return defaultResult();
        }
        out.append("over()");
        return defaultResult();
    }

    @Override
    public Void visitPartitionBy(PartitionBy p) {
        out.append("partition(").comma(p.items(), this::appendNode).append(")");
        return defaultResult();
    }

    @Override
    public Void visitFrameSingle(FrameSpec.Single f) {
        String fn = switch (f.unit()) {
            case ROWS -> "rows";
            case RANGE -> "range";
            case GROUPS -> "groups";
        };
        out.append(fn).append("(");
        appendNode(f.bound());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitFrameBetween(FrameSpec.Between f) {
        String fn = switch (f.unit()) {
            case ROWS -> "rows";
            case RANGE -> "range";
            case GROUPS -> "groups";
        };
        out.append(fn).append("(");
        appendNode(f.start());
        out.append(", ");
        appendNode(f.end());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitBoundUnboundedPreceding(BoundSpec.UnboundedPreceding b) {
        out.append("unboundedPreceding()");
        return defaultResult();
    }

    @Override
    public Void visitBoundPreceding(BoundSpec.Preceding b) {
        out.append("preceding(");
        appendNode(b.expr());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitBoundCurrentRow(BoundSpec.CurrentRow b) {
        out.append("currentRow()");
        return defaultResult();
    }

    @Override
    public Void visitBoundFollowing(BoundSpec.Following b) {
        out.append("following(");
        appendNode(b.expr());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitBoundUnboundedFollowing(BoundSpec.UnboundedFollowing b) {
        out.append("unboundedFollowing()");
        return defaultResult();
    }

    @Override
    public Void visitDistinctSpec(DistinctSpec d) {
        if (d.items() == null || d.items().isEmpty()) {
            out.append("distinct()");
            return defaultResult();
        }
        out.append("distinctOn(").comma(d.items(), this::appendNode).append(")");
        return defaultResult();
    }

    @Override
    public Void visitCaseExpr(CaseExpr c) {
        out.append("kase(").nl();
        out.in();
        out.comma(c.whens(), w -> appendNode(w));
        if (c.whens().size() > 1) {
            out.nl();
        }
        out.out();
        out.append(")");
        if (c.elseExpr() != null) {
            out.nl().append(".elseExpr(");
            appendNode(c.elseExpr());
            out.append(")");
        }
        return defaultResult();
    }

    @Override
    public Void visitWhenThen(WhenThen w) {
        out.append("when(");
        appendNode(w.when());
        out.append(")");
        if (w.then() != null) {
            out.append(".then(");
            appendNode(w.then());
            out.append(")");
        }
        return defaultResult();
    }

    @Override
    public Void visitTopSpec(TopSpec t) {
        if (t.percent() || t.withTies()) {
            out.append(".top(");
            appendNode(t.count());
            out.append(", ").append(String.valueOf(t.percent())).append(", ").append(String.valueOf(t.withTies())).append(")");
            return defaultResult();
        }
        out.append(".top(");
        appendNode(t.count());
        out.append(")");
        return defaultResult();
    }

    @Override
    public Void visitLimitOffset(LimitOffset l) {
        if (l.limitAll()) {
            if (l.offset() == null) {
                out.append(".limitOffset(limitAll())");
                return defaultResult();
            }
            out.append(".limitOffset(limitAll(");
            appendNode(l.offset());
            out.append("))");
            return defaultResult();
        }
        if (l.limit() != null && l.offset() != null) {
            out.append(".limitOffset(limitOffset(");
            appendNode(l.limit());
            out.append(", ");
            appendNode(l.offset());
            out.append("))");
            return defaultResult();
        }
        if (l.limit() != null) {
            out.append(".limit(");
            appendNode(l.limit());
            out.append(")");
            return defaultResult();
        }
        if (l.offset() != null) {
            out.append(".offset(");
            appendNode(l.offset());
            out.append(")");
            return defaultResult();
        }
        out.append(".limitOffset(limitOffset(null, null))");
        return defaultResult();
    }


    @Override
    public Void visitLockingClause(LockingClause lock) {
        appendLockMode(lock.mode());
        out.append(", ");
        appendLockTargets(lock.ofTables());
        out.append(", ").append(String.valueOf(lock.nowait())).append(", ").append(String.valueOf(lock.skipLocked()));
        return defaultResult();
    }

    @Override
    public Void visitAddArithmeticExpr(AddArithmeticExpr expr) {
        appendNode(expr.lhs());
        out.append(".add(");
        appendNode(expr.rhs());
        out.append(")");
        return defaultResult();
    }
}
