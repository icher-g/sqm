package io.sqm.transpile.builtin;

import io.sqm.core.*;
import io.sqm.core.walk.RecursiveNodeVisitor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Inspects statements for feature usage that may need transpilation handling.
 */
final class StatementFeatureInspector {
    private StatementFeatureInspector() {
    }

    static boolean hasResultClause(Statement statement) {
        var found = new AtomicBoolean(false);
        statement.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitInsertStatement(InsertStatement statement) {
                if (statement.result() != null) {
                    found.set(true);
                }
                return super.visitInsertStatement(statement);
            }

            @Override
            public Void visitUpdateStatement(UpdateStatement statement) {
                if (statement.result() != null) {
                    found.set(true);
                }
                return super.visitUpdateStatement(statement);
            }

            @Override
            public Void visitDeleteStatement(DeleteStatement statement) {
                if (statement.result() != null) {
                    found.set(true);
                }
                return super.visitDeleteStatement(statement);
            }

            @Override
            public Void visitMergeStatement(MergeStatement statement) {
                if (statement.result() != null) {
                    found.set(true);
                }
                return super.visitMergeStatement(statement);
            }
        });
        return found.get();
    }

    static boolean hasResultClauseWithoutVariableTarget(Statement statement) {
        var found = new AtomicBoolean(false);
        statement.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitResultClause(ResultClause clause) {
                if (!(clause.target() instanceof VariableResultTarget)) {
                    found.set(true);
                }
                return super.visitResultClause(clause);
            }
        });
        return found.get();
    }

    static boolean hasVariableResultTarget(Statement statement) {
        var found = new AtomicBoolean(false);
        statement.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitResultClause(ResultClause clause) {
                if (clause.target() instanceof VariableResultTarget) {
                    found.set(true);
                }
                return super.visitResultClause(clause);
            }
        });
        return found.get();
    }

    static boolean hasSqlServerOutputClause(Statement statement) {
        var found = new AtomicBoolean(false);
        statement.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitResultClause(ResultClause clause) {
                if (clause.target() instanceof RelationResultTarget || clause.usesDialectSpecificResultItems()) {
                    found.set(true);
                }
                return super.visitResultClause(clause);
            }
        });
        return found.get();
    }

    static boolean hasMergeStatement(Statement statement) {
        return statement instanceof MergeStatement;
    }

    static boolean hasMergeDoNothingAction(Statement statement) {
        var found = new AtomicBoolean(false);
        statement.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitMergeDoNothingAction(MergeDoNothingAction action) {
                found.set(true);
                return super.visitMergeDoNothingAction(action);
            }
        });
        return found.get();
    }

    static boolean hasMergeNotMatchedBySourceClause(Statement statement) {
        var found = new AtomicBoolean(false);
        statement.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitMergeClause(MergeClause clause) {
                if (clause.matchType() == MergeClause.MatchType.NOT_MATCHED_BY_SOURCE) {
                    found.set(true);
                }
                return super.visitMergeClause(clause);
            }
        });
        return found.get();
    }

    static boolean hasDistinctOn(Statement statement) {
        var found = new AtomicBoolean(false);
        statement.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitDistinctSpec(DistinctSpec spec) {
                if (!spec.items().isEmpty()) {
                    found.set(true);
                }
                super.visitDistinctSpec(spec);
                return null;
            }
        });
        return found.get();
    }

    static boolean hasSequenceValueExpression(Statement statement) {
        var found = new AtomicBoolean(false);
        statement.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitSequenceValueExpr(SequenceValueExpr expr) {
                found.set(true);
                return super.visitSequenceValueExpr(expr);
            }
        });
        return found.get();
    }

    static boolean hasCurrentSequenceValueExpression(Statement statement) {
        var found = new AtomicBoolean(false);
        statement.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitSequenceValueExpr(SequenceValueExpr expr) {
                if (expr.kind() == SequenceValueKind.CURRENT_VALUE) {
                    found.set(true);
                }
                return super.visitSequenceValueExpr(expr);
            }
        });
        return found.get();
    }

    static boolean hasHierarchicalQuery(Statement statement) {
        var found = new AtomicBoolean(false);
        statement.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitHierarchicalQueryClause(HierarchicalQueryClause clause) {
                found.set(true);
                return super.visitHierarchicalQueryClause(clause);
            }
        });
        return found.get();
    }

    static boolean hasPivotOrUnpivotTable(Statement statement) {
        var found = new AtomicBoolean(false);
        statement.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitPivotTable(PivotTable table) {
                found.set(true);
                return super.visitPivotTable(table);
            }

            @Override
            public Void visitUnpivotTable(UnpivotTable table) {
                found.set(true);
                return super.visitUnpivotTable(table);
            }
        });
        return found.get();
    }

    static boolean hasTableAccessModifier(Statement statement) {
        var found = new AtomicBoolean(false);
        statement.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitTable(Table table) {
                if (table.version() != null || table.partitionSpec() != null) {
                    found.set(true);
                }
                return super.visitTable(table);
            }

            @Override
            public Void visitSampledTable(SampledTable table) {
                found.set(true);
                return super.visitSampledTable(table);
            }
        });
        return found.get();
    }

    static boolean hasLikeMode(Statement statement, LikeMode mode) {
        var found = new AtomicBoolean(false);
        statement.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitLikePredicate(LikePredicate predicate) {
                if (predicate.mode() == mode) {
                    found.set(true);
                }
                return super.visitLikePredicate(predicate);
            }
        });
        return found.get();
    }

    static boolean hasAnyBinaryOperator(Statement statement, Set<String> operators) {
        var found = new AtomicBoolean(false);
        statement.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitBinaryOperatorExpr(BinaryOperatorExpr expr) {
                if (operators.contains(expr.operator().symbol())) {
                    found.set(true);
                }
                return super.visitBinaryOperatorExpr(expr);
            }
        });
        return found.get();
    }

    static boolean hasAnyFunctionNamePrefix(Statement statement, Set<String> prefixes) {
        var normalized = prefixes.stream().map(String::toUpperCase).collect(java.util.stream.Collectors.toUnmodifiableSet());
        var found = new AtomicBoolean(false);
        statement.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitFunctionExpr(FunctionExpr function) {
                var name = function.name().values().isEmpty()
                    ? ""
                    : function.name().values().getLast().toUpperCase();
                if (normalized.stream().anyMatch(name::startsWith)) {
                    found.set(true);
                }
                return super.visitFunctionExpr(function);
            }
        });
        return found.get();
    }

    static boolean hasFunctionTable(Statement statement) {
        var found = new AtomicBoolean(false);
        statement.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitFunctionTable(FunctionTable table) {
                found.set(true);
                super.visitFunctionTable(table);
                return null;
            }
        });
        return found.get();
    }

    static List<LocatedPatternRecognition> patternRecognitionTables(Statement statement) {
        return new PatternRecognitionLocationVisitor().find(statement);
    }

    static boolean hasInsertMode(Statement statement, InsertStatement.InsertMode mode) {
        var found = new AtomicBoolean(false);
        statement.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitInsertStatement(InsertStatement statement) {
                if (statement.insertMode() == mode) {
                    found.set(true);
                }
                return super.visitInsertStatement(statement);
            }
        });
        return found.get();
    }

    static boolean hasOnConflictAction(Statement statement, InsertStatement.OnConflictAction action) {
        var found = new AtomicBoolean(false);
        statement.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitInsertStatement(InsertStatement statement) {
                if (statement.onConflictAction() == action) {
                    found.set(true);
                }
                return super.visitInsertStatement(statement);
            }
        });
        return found.get();
    }

    record LocatedPatternRecognition(PatternRecognitionTable table, String path) {
    }

    private static final class PatternRecognitionLocationVisitor extends RecursiveNodeVisitor<Void> {
        private final ArrayDeque<String> segments = new ArrayDeque<>();
        private final Map<String, Integer> occurrences = new LinkedHashMap<>();
        private final List<LocatedPatternRecognition> located = new ArrayList<>();

        List<LocatedPatternRecognition> find(Statement statement) {
            accept(statement);
            return List.copyOf(located);
        }

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        protected Void accept(Node node) {
            if (node == null) {
                return null;
            }
            var segment = pathSegment(node);
            if (segment != null) {
                segments.addLast(segment);
            }
            try {
                return node.accept(this);
            }
            finally {
                if (segment != null) {
                    segments.removeLast();
                }
            }
        }

        @Override
        public Void visitPatternRecognitionTable(PatternRecognitionTable table) {
            var basePath = String.join(".", segments);
            var occurrence = occurrences.merge(basePath, 1, Integer::sum) - 1;
            located.add(new LocatedPatternRecognition(table, basePath + "[" + occurrence + "]"));
            return super.visitPatternRecognitionTable(table);
        }

        private static String pathSegment(Node node) {
            return switch (node) {
                case InsertStatement ignored -> "insert";
                case UpdateStatement ignored -> "update";
                case DeleteStatement ignored -> "delete";
                case MergeStatement ignored -> "merge";
                case SelectQuery ignored -> "select";
                case CompositeQuery ignored -> "set";
                case WithQuery ignored -> "with";
                case CteDef ignored -> "cte";
                case QueryTable ignored -> "queryTable";
                case QueryExpr ignored -> "subquery";
                case Join ignored -> "join";
                case PatternRecognitionTable ignored -> "matchRecognize";
                case Lateral ignored -> "lateral";
                case PivotTable ignored -> "pivot";
                case UnpivotTable ignored -> "unpivot";
                case SampledTable ignored -> "sampled";
                case JsonTable ignored -> "jsonTable";
                case ExistsPredicate ignored -> "exists";
                default -> null;
            };
        }
    }
}
