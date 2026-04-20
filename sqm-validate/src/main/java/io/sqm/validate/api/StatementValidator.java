package io.sqm.validate.api;

import io.sqm.core.Statement;
import io.sqm.core.StatementSequence;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Validates a statement model semantically against an external contract.
 */
public interface StatementValidator extends Validator<Statement, ValidationResult> {
    /**
     * Validates all statements in a statement sequence.
     *
     * @param sequence statement sequence to validate.
     * @return validation result containing problems from all statements.
     */
    default ValidationResult validate(StatementSequence sequence) {
        Objects.requireNonNull(sequence, "sequence");
        var problems = new ArrayList<ValidationProblem>();
        for (int i = 0; i < sequence.statements().size(); i++) {
            int statementIndex = i + 1;
            validate(sequence.statements().get(i)).problems().stream()
                .map(problem -> problem.withStatementIndex(statementIndex))
                .forEach(problems::add);
        }
        return new ValidationResult(problems);
    }
}
