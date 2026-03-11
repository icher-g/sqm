package io.sqm.validate.api;

import io.sqm.core.Statement;

/**
 * Validates a statement model semantically against an external contract.
 */
public interface StatementValidator extends Validator<Statement, ValidationResult> {
}
