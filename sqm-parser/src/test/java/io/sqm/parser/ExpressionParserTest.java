package io.sqm.parser;

import io.sqm.core.Expression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ExpressionParser.
 * Note: This is a simple wrapper class that delegates to BinaryOperatorExpr parsing.
 * More comprehensive expression parsing tests are in sqm-parser-ansi module.
 */
class ExpressionParserTest {

    @Test
    void targetType_returnsExpressionClass() {
        ExpressionParser parser = new ExpressionParser();
        assertEquals(Expression.class, parser.targetType());
    }

    @Test
    void parser_isStateless() {
        ExpressionParser parser = new ExpressionParser();
        
        // The parser should be stateless and reusable
        assertNotNull(parser);
        assertEquals(Expression.class, parser.targetType());
        assertEquals(Expression.class, parser.targetType()); // Call again, should be same
    }

    @Test
    void instantiation_succeeds() {
        // Simple test to ensure the class can be instantiated
        assertDoesNotThrow(() -> new ExpressionParser());
    }
}
