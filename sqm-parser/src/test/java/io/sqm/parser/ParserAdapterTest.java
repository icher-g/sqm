package io.sqm.parser;

import io.sqm.core.ColumnExpr;
import io.sqm.core.Expression;
import io.sqm.core.Identifier;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParserAdapterTest {

    @Test
    void widen_exposesRequestedTargetTypeAndDelegatesParsing() {
        Parser<ColumnExpr> delegate = new Parser<>() {
            @Override
            public ParseResult<? extends ColumnExpr> parse(Cursor cur, ParseContext ctx) {
                return ParseResult.ok(ColumnExpr.of(null, Identifier.of("name")));
            }

            @Override
            public Class<ColumnExpr> targetType() {
                return ColumnExpr.class;
            }
        };

        Parser<Expression> adapter = ParserAdapter.widen(Expression.class, delegate);
        var ctx = TestSupport.context(new DefaultParsersRepository());
        var result = adapter.parse(Cursor.of("", ctx.identifierQuoting()), ctx);

        assertEquals(Expression.class, adapter.targetType());
        var expr = assertInstanceOf(ColumnExpr.class, result.value());
        assertEquals("name", expr.name().value());
    }

    @Test
    void widen_rejectsNullArguments() {
        Parser<ColumnExpr> delegate = new Parser<>() {
            @Override
            public ParseResult<? extends ColumnExpr> parse(Cursor cur, ParseContext ctx) {
                return ParseResult.ok(ColumnExpr.of(null, Identifier.of("name")));
            }

            @Override
            public Class<ColumnExpr> targetType() {
                return ColumnExpr.class;
            }
        };

        assertThrows(NullPointerException.class, () -> ParserAdapter.widen(null, delegate));
        assertThrows(NullPointerException.class, () -> ParserAdapter.widen(Expression.class, null));
    }
}
