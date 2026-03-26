package io.sqm.transpile.builtin;

import io.sqm.core.BinaryOperatorExpr;
import io.sqm.core.DeleteStatement;
import io.sqm.core.DistinctSpec;
import io.sqm.core.FunctionExpr;
import io.sqm.core.InsertStatement;
import io.sqm.core.LikeMode;
import io.sqm.core.LikePredicate;
import io.sqm.core.MergeStatement;
import io.sqm.core.OutputColumnExpr;
import io.sqm.core.OutputStarResultItem;
import io.sqm.core.ResultClause;
import io.sqm.core.Statement;
import io.sqm.core.Table;
import io.sqm.core.TopSpec;
import io.sqm.core.SelectQuery;
import io.sqm.core.UpdateStatement;
import io.sqm.core.walk.RecursiveNodeVisitor;

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

    static boolean hasSqlServerOutputClause(Statement statement) {
        var found = new AtomicBoolean(false);
        statement.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitResultClause(ResultClause clause) {
                if (clause.into() != null) {
                    found.set(true);
                }
                return super.visitResultClause(clause);
            }

            @Override
            public Void visitOutputColumnExpr(OutputColumnExpr c) {
                found.set(true);
                return super.visitOutputColumnExpr(c);
            }

            @Override
            public Void visitOutputStarResultItem(OutputStarResultItem i) {
                found.set(true);
                return super.visitOutputStarResultItem(i);
            }
        });
        return found.get();
    }

    static boolean hasMergeStatement(Statement statement) {
        return statement instanceof MergeStatement;
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

    static boolean hasTopSpec(Statement statement) {
        var found = new AtomicBoolean(false);
        statement.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitTopSpec(TopSpec spec) {
                found.set(true);
                super.visitTopSpec(spec);
                return null;
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

    static boolean hasAnyFunctionName(Statement statement, Set<String> functionNames) {
        var normalized = functionNames.stream().map(String::toUpperCase).collect(java.util.stream.Collectors.toUnmodifiableSet());
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
                if (normalized.contains(name)) {
                    found.set(true);
                }
                return super.visitFunctionExpr(function);
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

    static boolean hasStatementHints(Statement statement) {
        var found = new AtomicBoolean(false);
        statement.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitSelectQuery(SelectQuery query) {
                if (!query.hints().isEmpty()) {
                    found.set(true);
                }
                return super.visitSelectQuery(query);
            }

            @Override
            public Void visitUpdateStatement(UpdateStatement statement) {
                if (!statement.hints().isEmpty()) {
                    found.set(true);
                }
                return super.visitUpdateStatement(statement);
            }

            @Override
            public Void visitDeleteStatement(DeleteStatement statement) {
                if (!statement.hints().isEmpty()) {
                    found.set(true);
                }
                return super.visitDeleteStatement(statement);
            }

            @Override
            public Void visitInsertStatement(InsertStatement statement) {
                if (!statement.hints().isEmpty()) {
                    found.set(true);
                }
                return super.visitInsertStatement(statement);
            }

            @Override
            public Void visitMergeStatement(MergeStatement statement) {
                if (!statement.hints().isEmpty()) {
                    found.set(true);
                }
                return super.visitMergeStatement(statement);
            }
        });
        return found.get();
    }

    static boolean hasIndexHints(Statement statement) {
        var found = new AtomicBoolean(false);
        statement.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitTable(Table table) {
                if (table.hints().stream().anyMatch(h -> h.name().value().matches("^(USE|IGNORE|FORCE)_INDEX(_FOR_(JOIN|ORDER_BY|GROUP_BY))?$"))) {
                    found.set(true);
                }
                return super.visitTable(table);
            }
        });
        return found.get();
    }

    static boolean hasLockHints(Statement statement) {
        var found = new AtomicBoolean(false);
        statement.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitTable(Table table) {
                if (table.hints().stream().anyMatch(h -> switch (h.name().value()) {
                    case "NOLOCK", "UPDLOCK", "HOLDLOCK" -> true;
                    default -> false;
                })) {
                    found.set(true);
                }
                return super.visitTable(table);
            }
        });
        return found.get();
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
}
