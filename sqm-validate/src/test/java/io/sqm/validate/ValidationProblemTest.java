package io.sqm.validate;

import io.sqm.validate.api.ValidationProblem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidationProblemTest {

    @Test
    void constructorsDefaultStatementIndexToNull() {
        var simple = new ValidationProblem(ValidationProblem.Code.COLUMN_NOT_FOUND, "Missing column");
        var contextual = new ValidationProblem(
            ValidationProblem.Code.TYPE_MISMATCH,
            "Type mismatch",
            "ComparisonPredicate",
            "where"
        );

        assertNull(simple.nodeKind());
        assertNull(simple.clausePath());
        assertNull(simple.statementIndex());
        assertEquals("ComparisonPredicate", contextual.nodeKind());
        assertEquals("where", contextual.clausePath());
        assertNull(contextual.statementIndex());
    }

    @Test
    void withStatementIndexCopiesContextAndRejectsInvalidIndex() {
        var problem = new ValidationProblem(
            ValidationProblem.Code.COLUMN_NOT_FOUND,
            "Missing column",
            "ColumnExpr",
            "select.items"
        );

        var indexed = problem.withStatementIndex(3);

        assertEquals(problem.code(), indexed.code());
        assertEquals(problem.message(), indexed.message());
        assertEquals(problem.nodeKind(), indexed.nodeKind());
        assertEquals(problem.clausePath(), indexed.clausePath());
        assertEquals(3, indexed.statementIndex());
        assertNull(problem.statementIndex());
        assertThrows(IllegalArgumentException.class, () -> problem.withStatementIndex(0));
    }
}
