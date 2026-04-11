package io.sqm.codegen;

import io.sqm.core.Statement;
import io.sqm.parser.spi.IdentifierQuoting;
import io.sqm.parser.spi.ParseResult;

/**
 * Captures one dialect-specific parse attempt made while generating Java DSL output.
 *
 * @param stage parse stage name
 * @param result parse result for the stage
 * @param identifierQuoting identifier quoting rules used for this attempt
 */
record ParseAttempt(String stage, ParseResult<? extends Statement> result, IdentifierQuoting identifierQuoting) {
}
