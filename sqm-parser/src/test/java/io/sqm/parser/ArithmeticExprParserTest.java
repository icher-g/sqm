package io.sqm.parser;

import io.sqm.core.AddArithmeticExpr;
import io.sqm.core.AdditiveArithmeticExpr;
import io.sqm.core.ArithmeticExpr;
import io.sqm.core.Expression;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArithmeticExprParserTest {

    @Test
    void delegatesToAdditiveParser() {
        var ctx = contextWithAdditiveParser();

        var result = ctx.parse(ArithmeticExpr.class, "add");

        assertTrue(result.ok());
        assertInstanceOf(AdditiveArithmeticExpr.class, result.value());
    }

    @Test
    void targetTypeIsArithmeticExpr() {
        var parser = new ArithmeticExprParser();

        assertEquals(ArithmeticExpr.class, parser.targetType());
    }

    private static ParseContext contextWithAdditiveParser() {
        var repo = new DefaultParsersRepository()
            .register(ArithmeticExpr.class, new ArithmeticExprParser())
            .register(AdditiveArithmeticExpr.class, new AdditiveArithmeticExprParser());
        return TestSupport.context(repo);
    }

    private static final class AdditiveArithmeticExprParser implements Parser<AdditiveArithmeticExpr> {
        @Override
        public ParseResult<? extends AdditiveArithmeticExpr> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected marker", TokenType.IDENT);
            return ParseResult.ok(AddArithmeticExpr.of(Expression.literal(1), Expression.literal(2)));
        }

        @Override
        public Class<AdditiveArithmeticExpr> targetType() {
            return AdditiveArithmeticExpr.class;
        }
    }
}
