package io.sqm.codegen;

import io.sqm.core.*;
import io.sqm.core.walk.RecursiveNodeVisitor;

import java.util.List;

final class SqmJavaEmitter {
    private final DslEmitterVisitor visitor = new DslEmitterVisitor();

    String emitStatement(Statement statement) {
        if (statement instanceof SelectQuery selectQuery) {
            return visitor.emitTopLevelSelectQuery(selectQuery);
        }
        return visitor.emitNode(statement);
    }

    String emitQuery(Query query) {
        return emitStatement(query);
    }

    private static final class DslEmitterVisitor extends RecursiveNodeVisitor<String> {
        private static String emitIdentifier(Identifier value) {
            return value.quoteStyle() == QuoteStyle.NONE
                ? "id(" + quote(value.value()) + ")"
                : "id(" + quote(value.value()) + ", QuoteStyle." + value.quoteStyle().name() + ")";
        }

        private static String emitIdentifierList(List<Identifier> values) {
            return "java.util.List.of(" + joinInline(values.stream().map(DslEmitterVisitor::emitIdentifier).toList()) + ")";
        }

        private static String emitQualifiedName(QualifiedName value) {
            return "QualifiedName.of(" + joinInline(value.parts().stream().map(DslEmitterVisitor::emitIdentifier).toList()) + ")";
        }

        private static String emitLockHint(Table.LockHint hint) {
            return switch (hint.kind()) {
                case NOLOCK -> "withNoLock()";
                case UPDLOCK -> "withUpdLock()";
                case HOLDLOCK -> "withHoldLock()";
            };
        }

        private static String emitStringList(List<String> values) {
            return "java.util.List.of(" + joinInline(values.stream().map(DslEmitterVisitor::quote).toList()) + ")";
        }

        private static String emitLockMode(LockMode mode) {
            return switch (mode) {
                case UPDATE -> "update()";
                case NO_KEY_UPDATE -> "noKeyUpdate()";
                case SHARE -> "share()";
                case KEY_SHARE -> "keyShare()";
            };
        }

        private static String emitLockTargets(List<LockTarget> targets) {
            if (targets == null || targets.isEmpty()) {
                return "java.util.List.of()";
            }
            return "ofTables(" + joinInline(targets.stream().map(LockTarget::identifier).map(DslEmitterVisitor::quote).toList()) + ")";
        }

        private static String emitExcludeOrNull(OverSpec.Exclude exclude) {
            if (exclude == null) {
                return "null";
            }
            return switch (exclude) {
                case CURRENT_ROW -> "excludeCurrentRow()";
                case GROUP -> "excludeGroup()";
                case TIES -> "excludeTies()";
                case NO_OTHERS -> "excludeNoOthers()";
            };
        }

        private static String likeMethod(LikeMode mode, boolean negated) {
            return switch (mode) {
                case LIKE -> negated ? "notLike" : "like";
                case ILIKE -> negated ? "notIlike" : "ilike";
                case SIMILAR_TO -> negated ? "notSimilarTo" : "similarTo";
            };
        }

        private static String emitLiteralValue(Object value) {
            switch (value) {
                case null -> {
                    return "null";
                }
                case String stringValue -> {
                    return quote(stringValue);
                }
                case Character charValue -> {
                    return "'" + escapeJava(String.valueOf(charValue)) + "'";
                }
                case Long ignored -> {
                    return value + "L";
                }
                case Float ignored -> {
                    return value + "F";
                }
                case Short ignored -> {
                    return "(short)" + value;
                }
                case Byte ignored -> {
                    return "(byte)" + value;
                }
                default -> {
                }
            }
            if (value instanceof Boolean || value instanceof Integer || value instanceof Double) {
                return String.valueOf(value);
            }
            throw new IllegalStateException("Unsupported literal value type: " + value.getClass().getName());
        }

        private static String indentList(List<String> values, int level) {
            if (values.isEmpty()) {
                return "";
            }
            var pad = "  ".repeat(level);
            var sb = new StringBuilder();
            for (int i = 0; i < values.size(); i++) {
                sb.append(pad).append(values.get(i));
                if (i < values.size() - 1) {
                    sb.append(",\n");
                }
            }
            return sb.toString();
        }

        private static String joinInline(List<String> values) {
            return String.join(", ", values);
        }

        private static String quote(String value) {
            return "\"" + escapeJava(value) + "\"";
        }

        private static String quote(Identifier value) {
            return quote(value.value());
        }

        private static String escapeJava(String value) {
            return value.replace("\\", "\\\\").replace("\"", "\\\"");
        }

        private static IllegalStateException unsupported(String label, Object value) {
            return new IllegalStateException("Unsupported " + label + ": " + value.getClass().getName());
        }

        String emitNode(Node node) {
            String result = node.accept(this);
            if (result == null) {
                throw unsupported("node", node);
            }
            return result;
        }

        String emitTopLevelSelectQuery(SelectQuery q) {
            return emitSelectQuery(q, "builder.select(\n");
        }

        @Override
        protected String defaultResult() {
            return null;
        }

        @Override
        public String visitSelectQuery(SelectQuery q) {
            return emitSelectQuery(q, "select(\n");
        }

        @Override
        public String visitInsertStatement(InsertStatement statement) {
            var sb = new StringBuilder("insert(").append(emitNode(statement.table())).append(")");
            if (statement.insertMode() == InsertStatement.InsertMode.IGNORE) {
                sb.append(".ignore()");
            }
            if (statement.insertMode() == InsertStatement.InsertMode.REPLACE) {
                sb.append(".replace()");
            }
            if (!statement.columns().isEmpty()) {
                sb.append(".columns(")
                    .append(joinInline(statement.columns().stream().map(DslEmitterVisitor::emitIdentifier).toList()))
                    .append(")");
            }
            if (statement.source() instanceof Query query) {
                sb.append(".query(").append(emitNode(query)).append(")");
            }
            else {
                sb.append(".values(").append(emitNode(statement.source())).append(")");
            }
            switch (statement.onConflictAction()) {
                case NONE -> {
                }
                case DO_NOTHING -> {
                    if (statement.conflictTarget().isEmpty()) {
                        sb.append(".onConflictDoNothing()");
                    }
                    else {
                        sb.append(".onConflictDoNothing(")
                            .append(joinInline(statement.conflictTarget().stream().map(DslEmitterVisitor::emitIdentifier).toList()))
                            .append(")");
                    }
                }
                case DO_UPDATE -> {
                    if (statement.conflictTarget().isEmpty() && statement.conflictUpdateWhere() == null) {
                        sb.append(".onConflictDoUpdate(")
                            .append(joinInline(statement.conflictUpdateAssignments().stream().map(this::emitNode).toList()))
                            .append(")");
                    }
                    else {
                        sb.append(".onConflictDoUpdate(")
                            .append(emitIdentifierList(statement.conflictTarget()))
                            .append(", java.util.List.of(")
                            .append(joinInline(statement.conflictUpdateAssignments().stream().map(this::emitNode).toList()))
                            .append("), ")
                            .append(statement.conflictUpdateWhere() == null ? "null" : emitNode(statement.conflictUpdateWhere()))
                            .append(")");
                    }
                }
            }
            if (statement.result() != null) {
                sb.append(emitResultBuilderCall(statement.result()));
            }
            sb.append(".build()");
            return sb.toString();
        }

        @Override
        public String visitUpdateStatement(UpdateStatement statement) {
            var sb = new StringBuilder("update(").append(emitNode(statement.table())).append(")");
            if (!statement.optimizerHints().isEmpty()) {
                sb.append(".optimizerHints(").append(emitStringList(statement.optimizerHints())).append(")");
            }
            if (!statement.joins().isEmpty()) {
                sb.append(".joins(")
                    .append(joinInline(statement.joins().stream().map(this::emitNode).toList()))
                    .append(")");
            }
            for (var assignment : statement.assignments()) {
                sb.append(".set(").append(emitNode(assignment)).append(")");
            }
            if (!statement.from().isEmpty()) {
                sb.append(".from(")
                    .append(joinInline(statement.from().stream().map(this::emitNode).toList()))
                    .append(")");
            }
            if (statement.where() != null) {
                sb.append(".where(").append(emitNode(statement.where())).append(")");
            }
            if (statement.result() != null) {
                sb.append(emitResultBuilderCall(statement.result()));
            }
            sb.append(".build()");
            return sb.toString();
        }

        @Override
        public String visitDeleteStatement(DeleteStatement statement) {
            var sb = new StringBuilder("delete(").append(emitNode(statement.table())).append(")");
            if (!statement.optimizerHints().isEmpty()) {
                sb.append(".optimizerHints(").append(emitStringList(statement.optimizerHints())).append(")");
            }
            if (!statement.using().isEmpty()) {
                sb.append(".using(")
                    .append(joinInline(statement.using().stream().map(this::emitNode).toList()))
                    .append(")");
            }
            if (!statement.joins().isEmpty()) {
                sb.append(".joins(")
                    .append(joinInline(statement.joins().stream().map(this::emitNode).toList()))
                    .append(")");
            }
            if (statement.where() != null) {
                sb.append(".where(").append(emitNode(statement.where())).append(")");
            }
            if (statement.result() != null) {
                sb.append(emitResultBuilderCall(statement.result()));
            }
            sb.append(".build()");
            return sb.toString();
        }

        @Override
        public String visitMergeStatement(MergeStatement statement) {
            var sb = new StringBuilder("merge(").append(emitNode(statement.target())).append(")");
            sb.append(".source(").append(emitNode(statement.source())).append(")");
            sb.append(".on(").append(emitNode(statement.on())).append(")");
            for (var clause : statement.clauses()) {
                sb.append(emitMergeClauseBuilderCall(clause));
            }
            if (statement.result() != null) {
                sb.append(emitResultBuilderCall(statement.result()));
            }
            sb.append(".build()");
            return sb.toString();
        }

        private String emitMergeClauseBuilderCall(MergeClause clause) {
            if (clause.matchType() == MergeClause.MatchType.MATCHED && clause.action() instanceof MergeUpdateAction updateAction) {
                return ".whenMatchedUpdate(java.util.List.of("
                    + joinInline(updateAction.assignments().stream().map(this::emitNode).toList())
                    + "))";
            }
            if (clause.matchType() == MergeClause.MatchType.MATCHED && clause.action() instanceof MergeDeleteAction) {
                return ".whenMatchedDelete()";
            }
            if (clause.matchType() == MergeClause.MatchType.NOT_MATCHED && clause.action() instanceof MergeInsertAction insertAction) {
                if (insertAction.columns().isEmpty()) {
                    return ".whenNotMatchedInsert(" + emitNode(insertAction.values()) + ")";
                }
                return ".whenNotMatchedInsert("
                    + emitIdentifierList(insertAction.columns())
                    + ", "
                    + emitNode(insertAction.values())
                    + ")";
            }
            throw unsupported("merge clause", clause);
        }

        private String emitResultBuilderCall(ResultClause clause) {
            var items = joinInline(clause.items().stream().map(this::emitNode).toList());
            if (clause.into() == null) {
                return ".result(" + items + ")";
            }
            return ".result(" + emitNode(clause.into()) + ", " + items + ")";
        }

        @Override
        public String visitAssignment(Assignment assignment) {
            var target = assignment.column().parts().size() == 1
                ? emitIdentifier(assignment.column().parts().getFirst())
                : emitQualifiedName(assignment.column());
            return "set(" + target + ", " + emitNode(assignment.value()) + ")";
        }

        private String emitSelectQuery(SelectQuery q, String selectPrefix) {
            var sb = new StringBuilder();
            sb.append(selectPrefix);
            sb.append(indentList(q.items().stream().map(this::emitNode).toList(), 1));
            sb.append("\n)");

            if (q.from() != null) {
                sb.append("\n.from(").append(emitNode(q.from())).append(")");
            }
            if (q.joins() != null && !q.joins().isEmpty()) {
                sb.append("\n.join(\n");
                sb.append(indentList(q.joins().stream().map(this::emitNode).toList(), 1));
                sb.append("\n)");
            }
            if (q.where() != null) {
                sb.append("\n.where(").append(emitNode(q.where())).append(")");
            }
            if (q.groupBy() != null && q.groupBy().items() != null && !q.groupBy().items().isEmpty()) {
                sb.append("\n.groupBy(");
                sb.append(joinInline(q.groupBy().items().stream().map(this::emitNode).toList()));
                sb.append(")");
            }
            if (q.having() != null) {
                sb.append("\n.having(").append(emitNode(q.having())).append(")");
            }
            if (q.windows() != null && !q.windows().isEmpty()) {
                sb.append("\n.window(");
                sb.append(joinInline(q.windows().stream().map(this::emitNode).toList()));
                sb.append(")");
            }
            if (q.orderBy() != null && q.orderBy().items() != null && !q.orderBy().items().isEmpty()) {
                sb.append("\n.orderBy(");
                sb.append(joinInline(q.orderBy().items().stream().map(this::emitNode).toList()));
                sb.append(")");
            }
            if (q.distinct() != null) {
                sb.append("\n").append(emitDistinctTail(q.distinct()));
            }
            if (q.topSpec() != null) {
                sb.append("\n").append(emitTopSpecTail(q.topSpec()));
            }
            if (q.limitOffset() != null) {
                sb.append("\n").append(emitLimitOffsetTail(q.limitOffset()));
            }
            if (q.lockFor() != null) {
                sb.append("\n.lockFor(").append(emitLockingClauseArgs(q.lockFor())).append(")");
            }
            sb.append("\n.build()");
            return sb.toString();
        }

        @Override
        public String visitExprSelectItem(ExprSelectItem i) {
            String expr = emitNode(i.expr());
            return i.alias() == null ? expr : expr + ".as(" + quote(i.alias().value()) + ")";
        }

        @Override
        public String visitStarSelectItem(StarSelectItem i) {
            return "star()";
        }

        @Override
        public String visitQualifiedStarSelectItem(QualifiedStarSelectItem i) {
            return "star(" + quote(i.qualifier().value()) + ")";
        }

        @Override
        public String visitTable(Table t) {
            StringBuilder out = new StringBuilder(t.schema() == null
                ? "tbl(" + quote(t.name()) + ")"
                : "tbl(" + quote(t.schema()) + ", " + quote(t.name()) + ")");
            if (t.alias() != null) {
                out.append(".as(").append(quote(t.alias())).append(")");
            }
            if (t.inheritance() == Table.Inheritance.ONLY) {
                out.append(".only()");
            }
            if (t.inheritance() == Table.Inheritance.INCLUDE_DESCENDANTS) {
                out.append(".includingDescendants()");
            }
            for (var hint : t.lockHints()) {
                out.append(".").append(emitLockHint(hint));
            }
            return out.toString();
        }

        @Override
        public String visitQueryTable(QueryTable t) {
            String out = "tbl(" + emitNode(t.query()) + ")";
            if (t.alias() != null) {
                out += ".as(" + quote(t.alias()) + ")";
            }
            if (t.columnAliases() != null && !t.columnAliases().isEmpty()) {
                out += ".columnAliases(" + joinInline(t.columnAliases().stream().map(DslEmitterVisitor::quote).toList()) + ")";
            }
            return out;
        }

        @Override
        public String visitLateral(Lateral l) {
            return emitNode(l.inner()) + ".lateral()";
        }

        @Override
        public String visitOnJoin(OnJoin j) {
            String fn = switch (j.kind()) {
                case INNER -> "inner";
                case LEFT -> "left";
                case RIGHT -> "right";
                case FULL -> "full";
                case STRAIGHT -> "straight";
            };
            String out = fn + "(" + emitNode(j.right()) + ")";
            if (j.on() != null) {
                out += ".on(" + emitNode(j.on()) + ")";
            }
            return out;
        }

        @Override
        public String visitCrossJoin(CrossJoin j) {
            return "cross(" + emitNode(j.right()) + ")";
        }

        @Override
        public String visitNaturalJoin(NaturalJoin j) {
            return "natural(" + emitNode(j.right()) + ")";
        }

        @Override
        public String visitUsingJoin(UsingJoin j) {
            String fn = switch (j.kind()) {
                case INNER -> "inner";
                case LEFT -> "left";
                case RIGHT -> "right";
                case FULL -> "full";
                case STRAIGHT -> "straight";
            };
            return fn + "(" + emitNode(j.right()) + ")"
                + ".using(" + joinInline(j.usingColumns().stream().map(DslEmitterVisitor::quote).toList()) + ")";
        }

        @Override
        public String visitSimpleGroupItem(GroupItem.SimpleGroupItem i) {
            return i.ordinal() == null
                ? "group(" + emitNode(i.expr()) + ")"
                : "group(" + i.ordinal() + ")";
        }

        @Override
        public String visitGroupingSet(GroupItem.GroupingSet i) {
            return "groupingSet(" + joinInline(i.items().stream().map(this::emitNode).toList()) + ")";
        }

        @Override
        public String visitGroupingSets(GroupItem.GroupingSets i) {
            return "groupingSets(" + joinInline(i.sets().stream().map(this::emitNode).toList()) + ")";
        }

        @Override
        public String visitRollup(GroupItem.Rollup i) {
            return "rollup(" + joinInline(i.items().stream().map(this::emitNode).toList()) + ")";
        }

        @Override
        public String visitCube(GroupItem.Cube i) {
            return "cube(" + joinInline(i.items().stream().map(this::emitNode).toList()) + ")";
        }

        @Override
        public String visitOrderItem(OrderItem i) {
            String out;
            if (i.expr() != null) {
                out = "order(" + emitNode(i.expr()) + ")";
            }
            else
                if (i.ordinal() != null) {
                    out = "order(" + i.ordinal() + ")";
                }
                else {
                    throw new IllegalStateException("Order item must have expression or ordinal");
                }
            if (i.direction() != null) {
                out += i.direction().name().equals("ASC") ? ".asc()" : ".desc()";
            }
            if (i.nulls() != null) {
                out += switch (i.nulls()) {
                    case FIRST -> ".nullsFirst()";
                    case LAST -> ".nullsLast()";
                    case DEFAULT -> ".nullsDefault()";
                };
            }
            if (i.collate() != null) {
                out += ".collate(" + quote(String.join(".", i.collate().values())) + ")";
            }
            if (i.usingOperator() != null) {
                out += ".using(" + quote(i.usingOperator()) + ")";
            }
            return out;
        }

        @Override
        public String visitComparisonPredicate(ComparisonPredicate p) {
            String fn = switch (p.operator()) {
                case EQ -> "eq";
                case NE -> "ne";
                case NULL_SAFE_EQ -> "nullSafeEq";
                case LT -> "lt";
                case LTE -> "lte";
                case GT -> "gt";
                case GTE -> "gte";
            };
            return emitNode(p.lhs()) + "." + fn + "(" + emitNode(p.rhs()) + ")";
        }

        @Override
        public String visitAndPredicate(AndPredicate p) {
            return emitNode(p.lhs()) + ".and(" + emitNode(p.rhs()) + ")";
        }

        @Override
        public String visitOrPredicate(OrPredicate p) {
            return emitNode(p.lhs()) + ".or(" + emitNode(p.rhs()) + ")";
        }

        @Override
        public String visitNotPredicate(NotPredicate p) {
            return emitNode(p.inner()) + ".not()";
        }

        @Override
        public String visitUnaryPredicate(UnaryPredicate p) {
            return "unary(" + emitNode(p.expr()) + ")";
        }

        @Override
        public String visitIsNullPredicate(IsNullPredicate p) {
            return p.negated() ? emitNode(p.expr()) + ".isNotNull()" : emitNode(p.expr()) + ".isNull()";
        }

        @Override
        public String visitInPredicate(InPredicate p) {
            return p.negated()
                ? emitNode(p.lhs()) + ".notIn(" + emitNode(p.rhs()) + ")"
                : emitNode(p.lhs()) + ".in(" + emitNode(p.rhs()) + ")";
        }

        @Override
        public String visitBetweenPredicate(BetweenPredicate p) {
            String out = emitNode(p.value()) + ".between(" + emitNode(p.lower()) + ", " + emitNode(p.upper()) + ")";
            if (p.symmetric()) {
                out += ".symmetric(true)";
            }
            if (p.negated()) {
                out += ".negated(true)";
            }
            return out;
        }

        @Override
        public String visitLikePredicate(LikePredicate p) {
            String fn = likeMethod(p.mode(), p.negated());
            String out = emitNode(p.value()) + "." + fn + "(" + emitNode(p.pattern()) + ")";
            if (p.escape() != null) {
                out += ".escape(" + emitNode(p.escape()) + ")";
            }
            return out;
        }

        @Override
        public String visitExistsPredicate(ExistsPredicate p) {
            return p.negated() ? "notExists(" + emitNode(p.subquery()) + ")" : "exists(" + emitNode(p.subquery()) + ")";
        }

        @Override
        public String visitColumnExpr(ColumnExpr c) {
            return c.tableAlias() == null
                ? "col(" + quote(c.name()) + ")"
                : "col(" + quote(c.tableAlias()) + ", " + quote(c.name()) + ")";
        }

        @Override
        public String visitOutputColumnExpr(OutputColumnExpr c) {
            var helper = c.source() == OutputRowSource.INSERTED ? "inserted" : "deleted";
            return helper + "(" + emitIdentifier(c.column()) + ")";
        }

        @Override
        public String visitExprResultItem(ExprResultItem item) {
            var expr = emitNode(item.expr());
            if (item.alias() != null) {
                return expr + ".as(" + emitIdentifier(item.alias()) + ")";
            }
            return expr;
        }

        @Override
        public String visitStarResultItem(StarResultItem item) {
            return "star()";
        }

        @Override
        public String visitQualifiedStarResultItem(QualifiedStarResultItem item) {
            return "star(" + emitIdentifier(item.qualifier()) + ")";
        }

        @Override
        public String visitOutputStarResultItem(OutputStarResultItem item) {
            return item.source() == OutputRowSource.INSERTED ? "insertedAll()" : "deletedAll()";
        }

        @Override
        public String visitResultInto(ResultInto into) {
            if (into.columns().isEmpty()) {
                return "resultInto(" + emitNode(into.target()) + ")";
            }
            return "resultInto(" + emitNode(into.target()) + ", "
                + joinInline(into.columns().stream().map(DslEmitterVisitor::emitIdentifier).toList())
                + ")";
        }

        @Override
        public String visitResultClause(ResultClause clause) {
            if (clause.into() == null) {
                return "result(" + joinInline(clause.items().stream().map(this::emitNode).toList()) + ")";
            }
            return "result(" + emitNode(clause.into()) + ", "
                + joinInline(clause.items().stream().map(this::emitNode).toList()) + ")";
        }

        @Override
        public String visitLiteralExpr(LiteralExpr l) {
            return "lit(" + emitLiteralValue(l.value()) + ")";
        }

        @Override
        public String visitAnonymousParamExpr(AnonymousParamExpr p) {
            return "param()";
        }

        @Override
        public String visitNamedParamExpr(NamedParamExpr p) {
            return "param(" + quote(p.name()) + ")";
        }

        @Override
        public String visitOrdinalParamExpr(OrdinalParamExpr p) {
            return "param(" + p.index() + ")";
        }

        @Override
        public String visitRowExpr(RowExpr v) {
            return "row(" + joinInline(v.items().stream().map(this::emitNode).toList()) + ")";
        }

        @Override
        public String visitRowListExpr(RowListExpr v) {
            return "rows(" + joinInline(v.rows().stream().map(this::emitNode).toList()) + ")";
        }

        @Override
        public String visitQueryExpr(QueryExpr v) {
            return "expr(" + emitNode(v.subquery()) + ")";
        }

        @Override
        public String visitFunctionArgExpr(FunctionExpr.Arg a) {
            if (a instanceof FunctionExpr.Arg.ExprArg exprArg) {
                return "arg(" + emitNode(exprArg.expr()) + ")";
            }
            if (a instanceof FunctionExpr.Arg.StarArg) {
                return "starArg()";
            }
            throw unsupported("function argument", a);
        }

        @Override
        public String visitFunctionExpr(FunctionExpr f) {
            String out = emitFunctionExprBase(f);
            if (Boolean.TRUE.equals(f.distinctArg())) {
                out += ".distinct()";
            }
            if (f.withinGroup() != null) {
                out += ".withinGroup(" + emitNode(f.withinGroup()) + ")";
            }
            if (f.filter() != null) {
                out += ".filter(" + emitNode(f.filter()) + ")";
            }
            if (f.over() != null) {
                out += ".over(" + emitNode(f.over()) + ")";
            }
            return out;
        }

        private String emitFunctionExprBase(FunctionExpr f) {
            var helper = emitSqlServerFunctionHelper(f);
            if (helper != null) {
                return helper;
            }
            String args = (f.args() == null || f.args().isEmpty())
                ? ""
                : ", " + joinInline(f.args().stream().map(this::emitNode).toList());
            return "func(" + quote(String.join(".", f.name().values())) + args + ")";
        }

        private String emitSqlServerFunctionHelper(FunctionExpr f) {
            if (f == null || f.name() == null) {
                return null;
            }
            var functionName = f.name().parts().getLast().value().toLowerCase(java.util.Locale.ROOT);
            return switch (functionName) {
                case "len" -> emitUnaryFunctionHelper("len", f);
                case "datalength" -> emitUnaryFunctionHelper("dataLength", f);
                case "getdate" -> (f.args() == null || f.args().isEmpty()) ? "getDate()" : null;
                case "dateadd" -> emitDatePartFunctionHelper("dateAdd", f);
                case "datediff" -> emitDatePartFunctionHelper("dateDiff", f);
                case "isnull" -> emitBinaryFunctionHelper("isNullFn", f);
                case "string_agg" -> emitBinaryFunctionHelper("stringAgg", f);
                default -> null;
            };
        }

        private String emitUnaryFunctionHelper(String helperName, FunctionExpr f) {
            if (f.args() == null || f.args().size() != 1) {
                return null;
            }
            return helperName + "(" + emitExprArg(f.args().getFirst()) + ")";
        }

        private String emitBinaryFunctionHelper(String helperName, FunctionExpr f) {
            if (f.args() == null || f.args().size() != 2) {
                return null;
            }
            return helperName + "(" + emitExprArg(f.args().getFirst()) + ", " + emitExprArg(f.args().get(1)) + ")";
        }

        private String emitDatePartFunctionHelper(String helperName, FunctionExpr f) {
            if (f.args() == null || f.args().size() != 3) {
                return null;
            }
            var firstArg = f.args().getFirst();
            if (!(firstArg instanceof FunctionExpr.Arg.ExprArg exprArg)
                || !(exprArg.expr() instanceof io.sqm.core.LiteralExpr literal)
                || !(literal.value() instanceof String datePart)) {
                return null;
            }
            return helperName + "(" + quote(datePart) + ", "
                + emitExprArg(f.args().get(1)) + ", "
                + emitExprArg(f.args().get(2)) + ")";
        }

        private String emitExprArg(FunctionExpr.Arg arg) {
            if (arg instanceof FunctionExpr.Arg.ExprArg exprArg) {
                return emitNode(exprArg.expr());
            }
            return emitNode(arg);
        }

        @Override
        public String visitOrderBy(OrderBy o) {
            return "orderBy(" + joinInline(o.items().stream().map(this::emitNode).toList()) + ")";
        }

        @Override
        public String visitWindowDef(WindowDef w) {
            return "window(" + quote(w.name().value()) + ", " + emitNode(w.spec()) + ")";
        }

        @Override
        public String visitOverRef(OverSpec.Ref r) {
            return "over(" + quote(r.windowName().value()) + ")";
        }

        @Override
        public String visitOverDef(OverSpec.Def d) {
            if (d.baseWindow() != null) {
                if (d.frame() != null && d.exclude() != null) {
                    return "over(" + quote(d.baseWindow().value()) + ", " + emitOrderByOrNull(d.orderBy())
                        + ", " + emitNode(d.frame()) + ", " + emitExcludeOrNull(d.exclude()) + ")";
                }
                if (d.frame() != null && d.orderBy() != null) {
                    return "over(" + quote(d.baseWindow().value()) + ", " + emitNode(d.orderBy()) + ", " + emitNode(d.frame()) + ")";
                }
                if (d.frame() != null) {
                    return "over(" + quote(d.baseWindow().value()) + ", " + emitNode(d.frame()) + ")";
                }
                if (d.orderBy() != null) {
                    return "over(" + quote(d.baseWindow().value()) + ", " + emitNode(d.orderBy()) + ")";
                }
                return "overDef(" + quote(d.baseWindow().value()) + ")";
            }
            if (d.partitionBy() != null) {
                if (d.frame() != null && d.exclude() != null) {
                    return "over(" + emitNode(d.partitionBy()) + ", " + emitOrderByOrNull(d.orderBy())
                        + ", " + emitNode(d.frame()) + ", " + emitExcludeOrNull(d.exclude()) + ")";
                }
                if (d.frame() != null && d.orderBy() != null) {
                    return "over(" + emitNode(d.partitionBy()) + ", " + emitNode(d.orderBy()) + ", " + emitNode(d.frame()) + ")";
                }
                if (d.frame() != null) {
                    return "over(" + emitNode(d.partitionBy()) + ", " + emitNode(d.frame()) + ")";
                }
                if (d.orderBy() != null) {
                    return "over(" + emitNode(d.partitionBy()) + ", " + emitNode(d.orderBy()) + ")";
                }
                return "over(" + emitNode(d.partitionBy()) + ")";
            }
            if (d.frame() != null && d.exclude() != null) {
                return "over(" + emitOrderByOrNull(d.orderBy()) + ", " + emitNode(d.frame()) + ", " + emitExcludeOrNull(d.exclude()) + ")";
            }
            if (d.frame() != null && d.orderBy() != null) {
                return "over(" + emitNode(d.orderBy()) + ", " + emitNode(d.frame()) + ")";
            }
            if (d.frame() != null) {
                return "over(" + emitNode(d.frame()) + ")";
            }
            if (d.orderBy() != null) {
                return "over(" + emitNode(d.orderBy()) + ")";
            }
            return "over()";
        }

        @Override
        public String visitPartitionBy(PartitionBy p) {
            return "partition(" + joinInline(p.items().stream().map(this::emitNode).toList()) + ")";
        }

        @Override
        public String visitFrameSingle(FrameSpec.Single f) {
            String fn = switch (f.unit()) {
                case ROWS -> "rows";
                case RANGE -> "range";
                case GROUPS -> "groups";
            };
            return fn + "(" + emitNode(f.bound()) + ")";
        }

        @Override
        public String visitFrameBetween(FrameSpec.Between f) {
            String fn = switch (f.unit()) {
                case ROWS -> "rows";
                case RANGE -> "range";
                case GROUPS -> "groups";
            };
            return fn + "(" + emitNode(f.start()) + ", " + emitNode(f.end()) + ")";
        }

        @Override
        public String visitBoundUnboundedPreceding(BoundSpec.UnboundedPreceding b) {
            return "unboundedPreceding()";
        }

        @Override
        public String visitBoundPreceding(BoundSpec.Preceding b) {
            return "preceding(" + emitNode(b.expr()) + ")";
        }

        @Override
        public String visitBoundCurrentRow(BoundSpec.CurrentRow b) {
            return "currentRow()";
        }

        @Override
        public String visitBoundFollowing(BoundSpec.Following b) {
            return "following(" + emitNode(b.expr()) + ")";
        }

        @Override
        public String visitBoundUnboundedFollowing(BoundSpec.UnboundedFollowing b) {
            return "unboundedFollowing()";
        }

        @Override
        public String visitDistinctSpec(DistinctSpec d) {
            if (d.items() == null || d.items().isEmpty()) {
                return "distinct()";
            }
            return "distinctOn(" + joinInline(d.items().stream().map(this::emitNode).toList()) + ")";
        }

        private String emitDistinctTail(DistinctSpec distinctSpec) {
            if (distinctSpec.items() == null || distinctSpec.items().isEmpty()) {
                return ".distinct(distinct())";
            }
            return ".distinct(" + joinInline(distinctSpec.items().stream().map(this::emitNode).toList()) + ")";
        }

        private String emitTopSpecTail(TopSpec topSpec) {
            if (topSpec.percent() || topSpec.withTies()) {
                return ".top(TopSpec.of("
                    + emitNode(topSpec.count())
                    + ", "
                    + topSpec.percent()
                    + ", "
                    + topSpec.withTies()
                    + "))";
            }
            return ".top(" + emitNode(topSpec.count()) + ")";
        }

        private String emitLimitOffsetTail(LimitOffset limitOffset) {
            if (limitOffset.limitAll()) {
                if (limitOffset.offset() == null) {
                    return ".limitOffset(limitAll())";
                }
                return ".limitOffset(limitAll(" + emitNode(limitOffset.offset()) + "))";
            }
            if (limitOffset.limit() != null && limitOffset.offset() != null) {
                return ".limitOffset(limitOffset(" + emitNode(limitOffset.limit()) + ", " + emitNode(limitOffset.offset()) + "))";
            }
            if (limitOffset.limit() != null) {
                return ".limit(" + emitNode(limitOffset.limit()) + ")";
            }
            if (limitOffset.offset() != null) {
                return ".offset(" + emitNode(limitOffset.offset()) + ")";
            }
            return ".limitOffset(limitOffset(null, null))";
        }

        private String emitLockingClauseArgs(LockingClause lock) {
            return emitLockMode(lock.mode())
                + ", "
                + emitLockTargets(lock.ofTables())
                + ", "
                + lock.nowait()
                + ", "
                + lock.skipLocked();
        }

        private String emitOrderByOrNull(OrderBy orderBy) {
            return orderBy == null ? "null" : emitNode(orderBy);
        }

    }
}
