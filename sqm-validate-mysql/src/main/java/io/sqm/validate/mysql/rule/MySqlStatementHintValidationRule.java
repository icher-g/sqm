package io.sqm.validate.mysql.rule;

import io.sqm.core.DeleteStatement;
import io.sqm.core.ExpressionHintArg;
import io.sqm.core.HintArg;
import io.sqm.core.IdentifierHintArg;
import io.sqm.core.InsertStatement;
import io.sqm.core.MergeStatement;
import io.sqm.core.QualifiedNameHintArg;
import io.sqm.core.SelectQuery;
import io.sqm.core.Statement;
import io.sqm.core.StatementHint;
import io.sqm.core.UpdateStatement;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.rule.SchemaValidationRule;

import java.util.Objects;

/**
 * Validates first-wave MySQL statement hint families structurally.
 */
public final class MySqlStatementHintValidationRule implements SchemaValidationRule<Statement> {
    private final DialectCapabilities capabilities;
    private final SqlDialectVersion version;

    /**
     * Creates a MySQL statement-hint validation rule.
     *
     * @param capabilities dialect capabilities
     * @param version MySQL version
     */
    public MySqlStatementHintValidationRule(DialectCapabilities capabilities, SqlDialectVersion version) {
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
        this.version = Objects.requireNonNull(version, "version");
    }

    @Override
    public Class<Statement> nodeType() {
        return Statement.class;
    }

    /**
     * Validates supported MySQL statement-hint names and argument shapes.
     *
     * @param node statement to validate.
     * @param context validation context.
     */
    @Override
    public void validate(Statement node, SchemaValidationContext context) {
        var clausePath = clausePath(node);
        if (clausePath == null) {
            return;
        }

        if (!node.hints().isEmpty() && !capabilities.supports(SqlFeature.OPTIMIZER_HINT_COMMENT)) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                "MySQL " + version + " does not support " + SqlFeature.OPTIMIZER_HINT_COMMENT.description(),
                node,
                clausePath
            );
            return;
        }

        for (var hint : node.hints()) {
            validateHint(hint, context, clausePath);
        }
    }

    private void validateHint(StatementHint hint, SchemaValidationContext context, String clausePath) {
        switch (hint.name().value()) {
            case "MAX_EXECUTION_TIME" -> validateMaxExecutionTime(hint, context, clausePath);
            case "BKA", "NO_BKA", "NO_RANGE_OPTIMIZATION" -> validateRelationTargetHint(hint, context, clausePath);
            case "SET_VAR" -> validateSetVar(hint, context, clausePath);
            case "QB_NAME" -> validateQueryBlockName(hint, context, clausePath);
            default -> context.addProblem(
                ValidationProblem.Code.DIALECT_FEATURE_UNSUPPORTED,
                "MySQL first-wave validation does not support statement hint " + hint.name().value(),
                hint,
                clausePath
            );
        }
    }

    private void validateMaxExecutionTime(StatementHint hint, SchemaValidationContext context, String clausePath) {
        if (hint.args().size() != 1 || !(hint.args().getFirst() instanceof ExpressionHintArg expressionArg)) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                "MySQL MAX_EXECUTION_TIME requires exactly one numeric expression argument",
                hint,
                clausePath
            );
            return;
        }

        var literal = expressionArg.value().matchExpression().literal(l -> l.value()).orElse(null);
        if (!(literal instanceof Number)) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                "MySQL MAX_EXECUTION_TIME requires a numeric literal argument",
                hint,
                clausePath
            );
        }
    }

    private void validateRelationTargetHint(StatementHint hint, SchemaValidationContext context, String clausePath) {
        if (hint.args().size() != 1 || !isIdentifierLike(hint.args().getFirst())) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                "MySQL " + hint.name().value() + " requires exactly one identifier-like relation argument",
                hint,
                clausePath
            );
        }
    }

    private void validateSetVar(StatementHint hint, SchemaValidationContext context, String clausePath) {
        if (hint.args().size() != 1 || !(hint.args().getFirst() instanceof IdentifierHintArg)) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                "MySQL SET_VAR requires exactly one assignment-like identifier argument",
                hint,
                clausePath
            );
        }
    }

    private void validateQueryBlockName(StatementHint hint, SchemaValidationContext context, String clausePath) {
        if (hint.args().size() != 1) {
            context.addProblem(
                ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                "MySQL QB_NAME requires exactly one query-block name argument",
                hint,
                clausePath
            );
            return;
        }

        var arg = hint.args().getFirst();
        if (arg instanceof IdentifierHintArg) {
            return;
        }
        if (arg instanceof ExpressionHintArg expressionArg) {
            var literal = expressionArg.value().matchExpression().literal(l -> l.value()).orElse(null);
            if (literal instanceof String) {
                return;
            }
        }

        context.addProblem(
            ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
            "MySQL QB_NAME requires a string or identifier argument",
            hint,
            clausePath
        );
    }

    private static boolean isIdentifierLike(HintArg arg) {
        return arg instanceof IdentifierHintArg || arg instanceof QualifiedNameHintArg;
    }

    private static String clausePath(Statement statement) {
        if (statement instanceof SelectQuery) {
            return "select.hint";
        }
        if (statement instanceof InsertStatement) {
            return "insert.hint";
        }
        if (statement instanceof UpdateStatement) {
            return "update.hint";
        }
        if (statement instanceof DeleteStatement) {
            return "delete.hint";
        }
        if (statement instanceof MergeStatement) {
            return "merge.hint";
        }
        return null;
    }
}
