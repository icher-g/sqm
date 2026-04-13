package io.sqm.codegen;

import io.sqm.parser.spi.ParseContext;

/**
 * Describes one named parse stage and its corresponding parse context.
 *
 * @param name stage name
 * @param context parse context used for the stage
 */
record ParseStage(String name, ParseContext context) {
}
