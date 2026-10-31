package io.sqm.validate.oracle.rule;

import io.sqm.core.*;
import io.sqm.core.walk.RecursiveNodeVisitor;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.ModelScope;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.rule.SchemaValidationRule;

/**
 * Checks MODEL's query-block restrictions, including recursive WITH members and locking.
 */
public final class ModelQueryCompatibilityValidationRule implements SchemaValidationRule<Statement> {
    /**
     * Creates the query-compatibility rule.
     */
    public ModelQueryCompatibilityValidationRule() {
    }

    private static boolean references(Query query, Identifier name) {
        var found = new boolean[1];
        query.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitTable(Table table) {
                found[0] |= table.schema() == null && ModelScope.key(table.name()).equals(ModelScope.key(name));
                return super.visitTable(table);
            }

            @Override
            public Void visitWithQuery(WithQuery with) {
                return with.ctes().stream().anyMatch(cte -> ModelScope.key(cte.name()).equals(ModelScope.key(name)))
                    ? null
                    : super.visitWithQuery(with);
            }
        });
        return found[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<Statement> nodeType() {
        return Statement.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(Statement statement, SchemaValidationContext context) {
        if (statement instanceof SelectQuery select && select.model() != null && select.lockFor() != null) {
            context.addProblem(ValidationProblem.Code.DIALECT_CLAUSE_INVALID,
                "MODEL output cannot be locked with FOR UPDATE", select.lockFor(), "model.lockFor");
        }
        if (!(statement instanceof WithQuery with)) return;
        for (var cteIndex = 0; cteIndex < with.ctes().size(); cteIndex++) {
            var cte = with.ctes().get(cteIndex);
            if (!(cte.body() instanceof CompositeQuery composite)) continue;
            for (var index = 1; index < composite.terms().size(); index++) {
                var term = composite.terms().get(index);
                if (!references(term, cte.name())) continue;
                var path = "with.ctes[" + cteIndex + "].terms[" + index + "].model";
                term.accept(new RecursiveNodeVisitor<Void>() {
                    @Override
                    protected Void defaultResult() {
                        return null;
                    }

                    @Override
                    public Void visitModelClause(ModelClause clause) {
                        context.addProblem(ValidationProblem.Code.DIALECT_CLAUSE_INVALID, "MODEL is not allowed in a recursive WITH member", clause, path);
                        return null;
                    }
                });
            }
        }
    }
}
