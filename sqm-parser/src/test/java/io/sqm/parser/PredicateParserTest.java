package io.sqm.parser;

import io.sqm.core.Expression;
import io.sqm.core.OrPredicate;
import io.sqm.core.Predicate;
import io.sqm.core.UnaryPredicate;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PredicateParserTest {

    @Test
    void delegatesToOrPredicate() {
        var ctx = contextWithOrPredicateParser();

        var result = ctx.parse(Predicate.class, "or");

        assertTrue(result.ok());
        assertInstanceOf(OrPredicate.class, result.value());
    }

    @Test
    void targetTypeIsPredicate() {
        var parser = new PredicateParser();

        assertEquals(Predicate.class, parser.targetType());
    }

    private static ParseContext contextWithOrPredicateParser() {
        var repo = new DefaultParsersRepository()
            .register(Predicate.class, new PredicateParser())
            .register(OrPredicate.class, new OrPredicateStubParser());
        return TestSupport.context(repo);
    }

    private static final class OrPredicateStubParser implements Parser<OrPredicate> {
        @Override
        public ParseResult<? extends OrPredicate> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected marker", TokenType.OR);
            var lhs = UnaryPredicate.of(Expression.literal(1));
            var rhs = UnaryPredicate.of(Expression.literal(2));
            return ParseResult.ok(OrPredicate.of(lhs, rhs));
        }

        @Override
        public Class<OrPredicate> targetType() {
            return OrPredicate.class;
        }
    }
}
