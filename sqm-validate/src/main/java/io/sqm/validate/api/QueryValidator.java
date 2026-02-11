package io.sqm.validate.api;

import io.sqm.core.Query;

/**
 * Validates a query model semantically against an external contract.
 */
public interface QueryValidator extends Validator<Query, ValidationResult> {
}
