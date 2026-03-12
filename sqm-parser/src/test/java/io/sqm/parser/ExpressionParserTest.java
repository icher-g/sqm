package io.sqm.parser;

import io.sqm.core.BinaryOperatorExpr;
import io.sqm.core.ConcatExpr;
import io.sqm.core.Expression;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ExpressionParser.
 * Note: This is a simple wrapper class that delegates to ConcatExpr parsing.
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

    @Test
    void delegatesToConcatExprParser() {
        var ctx = contextWithConcatParser(new ConcatOkParser());

        var result = ctx.parse(Expression.class, "bin");

        assertTrue(result.ok());
        assertInstanceOf(BinaryOperatorExpr.class, result.value());
    }

    @Test
    void propagatesConcatErrors() {
        var ctx = contextWithConcatParser(new ConcatErrorParser());

        var result = ctx.parse(Expression.class, "bin");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("concat error"));
    }

    private static ParseContext contextWithConcatParser(Parser<?> concatParser) {
        var repo = new DefaultParsersRepository()
            .register(Expression.class, new ExpressionParser())
            .register(ConcatExpr.class, concatParser);
        return TestSupport.context(repo);
    }

    private static final class ConcatOkParser implements Parser<Expression> {
        @Override
        public ParseResult<? extends Expression> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected marker", TokenType.IDENT);
            return ParseResult.ok(BinaryOperatorExpr.of(Expression.literal(1), "+", Expression.literal(2)));
        }

        @Override
        public Class<? extends Expression> targetType() {
            return ConcatExpr.class;
        }
    }

    private static final class ConcatErrorParser implements Parser<Expression> {
        @Override
        public ParseResult<? extends Expression> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected marker", TokenType.IDENT);
            return ParseResult.error("concat error", 0);
        }

        @Override
        public Class<? extends Expression> targetType() {
            return ConcatExpr.class;
        }
    }
}
