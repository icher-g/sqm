package io.sqm.transpile.builtin;

import io.sqm.core.BinaryOperatorExpr;
import io.sqm.core.DeleteStatement;
import io.sqm.core.DistinctSpec;
import io.sqm.core.FunctionExpr;
import io.sqm.core.InsertStatement;
import io.sqm.core.LikeMode;
import io.sqm.core.LikePredicate;
import io.sqm.core.Statement;
import io.sqm.core.Table;
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

    static boolean hasReturning(Statement statement) {
        var found = new AtomicBoolean(false);
        statement.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitInsertStatement(InsertStatement statement) {
                if (!statement.returning().isEmpty()) {
                    found.set(true);
                }
                return super.visitInsertStatement(statement);
            }

            @Override
            public Void visitUpdateStatement(UpdateStatement statement) {
                if (!statement.returning().isEmpty()) {
                    found.set(true);
                }
                return super.visitUpdateStatement(statement);
            }

            @Override
            public Void visitDeleteStatement(DeleteStatement statement) {
                if (!statement.returning().isEmpty()) {
                    found.set(true);
                }
                return super.visitDeleteStatement(statement);
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

    static boolean hasOptimizerHints(Statement statement) {
        var found = new AtomicBoolean(false);
        statement.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitSelectQuery(SelectQuery query) {
                if (!query.optimizerHints().isEmpty()) {
                    found.set(true);
                }
                return super.visitSelectQuery(query);
            }

            @Override
            public Void visitUpdateStatement(UpdateStatement statement) {
                if (!statement.optimizerHints().isEmpty()) {
                    found.set(true);
                }
                return super.visitUpdateStatement(statement);
            }

            @Override
            public Void visitDeleteStatement(DeleteStatement statement) {
                if (!statement.optimizerHints().isEmpty()) {
                    found.set(true);
                }
                return super.visitDeleteStatement(statement);
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
                if (!table.indexHints().isEmpty()) {
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
