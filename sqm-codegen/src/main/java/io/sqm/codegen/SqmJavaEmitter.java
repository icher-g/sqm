package io.sqm.codegen;

import io.sqm.core.*;
import io.sqm.core.walk.RecursiveNodeVisitor;

import java.util.List;

final class SqmJavaEmitter {
    private final DslEmitterVisitor visitor = new DslEmitterVisitor();

    String emitQuery(Query query) {
        if (query instanceof SelectQuery selectQuery) {
            return visitor.emitTopLevelSelectQuery(selectQuery);
        }
        return visitor.emitNode(query);
    }

    private static final class DslEmitterVisitor extends RecursiveNodeVisitor<String> {

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
            return i.alias() == null ? expr : expr + ".as(" + quote(i.alias()) + ")";
        }

        @Override
        public String visitStarSelectItem(StarSelectItem i) {
            return "star()";
        }

        @Override
        public String visitQualifiedStarSelectItem(QualifiedStarSelectItem i) {
            return "star(" + quote(i.qualifier()) + ")";
        }

        @Override
        public String visitTable(Table t) {
            String out = t.schema() == null
                ? "tbl(" + quote(t.name()) + ")"
                : "tbl(" + quote(t.schema()) + ", " + quote(t.name()) + ")";
            if (t.alias() != null) {
                out += ".as(" + quote(t.alias()) + ")";
            }
            if (t.inheritance() == Table.Inheritance.ONLY) {
                out += ".only()";
            }
            if (t.inheritance() == Table.Inheritance.INCLUDE_DESCENDANTS) {
                out += ".includingDescendants()";
            }
            return out;
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
                out += ".collate(" + quote(i.collate()) + ")";
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
            String args = (f.args() == null || f.args().isEmpty())
                ? ""
                : ", " + joinInline(f.args().stream().map(this::emitNode).toList());
            String out = "func(" + quote(f.name()) + args + ")";
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

        @Override
        public String visitOrderBy(OrderBy o) {
            return "orderBy(" + joinInline(o.items().stream().map(this::emitNode).toList()) + ")";
        }

        @Override
        public String visitWindowDef(WindowDef w) {
            return "window(" + quote(w.name()) + ", " + emitNode(w.spec()) + ")";
        }

        @Override
        public String visitOverRef(OverSpec.Ref r) {
            return "over(" + quote(r.windowName()) + ")";
        }

        @Override
        public String visitOverDef(OverSpec.Def d) {
            if (d.baseWindow() != null) {
                if (d.frame() != null && d.exclude() != null) {
                    return "over(" + quote(d.baseWindow()) + ", " + emitOrderByOrNull(d.orderBy())
                        + ", " + emitNode(d.frame()) + ", " + emitExcludeOrNull(d.exclude()) + ")";
                }
                if (d.frame() != null && d.orderBy() != null) {
                    return "over(" + quote(d.baseWindow()) + ", " + emitNode(d.orderBy()) + ", " + emitNode(d.frame()) + ")";
                }
                if (d.frame() != null) {
                    return "over(" + quote(d.baseWindow()) + ", " + emitNode(d.frame()) + ")";
                }
                if (d.orderBy() != null) {
                    return "over(" + quote(d.baseWindow()) + ", " + emitNode(d.orderBy()) + ")";
                }
                return "overDef(" + quote(d.baseWindow()) + ")";
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
