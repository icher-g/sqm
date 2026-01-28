package io.sqm.parser;

import io.sqm.core.ColumnExpr;
import io.sqm.core.Expression;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultParsersRepositoryTest {

    @Test
    void shouldRegisterAndRetrieveParser() {
        var repository = new DefaultParsersRepository();
        var parser = new TestParser();
        
        repository.register(Expression.class, parser);
        
        Parser<Expression> retrieved = repository.get(Expression.class);
        assertSame(parser, retrieved);
    }

    @Test
    void shouldReturnNullForUnregisteredType() {
        var repository = new DefaultParsersRepository();
        
        Parser<Expression> retrieved = repository.get(Expression.class);
        assertNull(retrieved);
    }

    @Test
    void shouldAllowMultipleRegistrations() {
        var repository = new DefaultParsersRepository();
        var parser1 = new TestParser();
        var parser2 = new TestParser();
        
        repository.register(Expression.class, parser1);
        repository.register(ColumnExpr.class, parser2);
        
        assertSame(parser1, repository.get(Expression.class));
        assertSame(parser2, repository.get(ColumnExpr.class));
    }

    @Test
    void shouldOverwriteExistingRegistration() {
        var repository = new DefaultParsersRepository();
        var parser1 = new TestParser();
        var parser2 = new TestParser();
        
        repository.register(Expression.class, parser1);
        repository.register(Expression.class, parser2);
        
        assertSame(parser2, repository.get(Expression.class));
    }

    @Test
    void shouldReturnSameRepositoryOnRegister() {
        var repository = new DefaultParsersRepository();
        var parser = new TestParser();
        
        ParsersRepository result = repository.register(Expression.class, parser);
        
        assertSame(repository, result);
    }

    @Test
    void shouldBeThreadSafe() {
        var repository = new DefaultParsersRepository();
        var parser1 = new TestParser();
        var parser2 = new TestParser();
        
        // Simulate concurrent registration
        Thread t1 = new Thread(() -> repository.register(Expression.class, parser1));
        Thread t2 = new Thread(() -> repository.register(ColumnExpr.class, parser2));
        
        t1.start();
        t2.start();
        
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            fail("Thread interrupted");
        }
        
        assertNotNull(repository.get(Expression.class));
        assertNotNull(repository.get(ColumnExpr.class));
    }

    private static class TestParser implements Parser<Expression> {
        @Override
        public ParseResult<Expression> parse(Cursor cursor, ParseContext ctx) {
            return ParseResult.ok(Expression.literal(1));
        }

        @Override
        public Class<Expression> targetType() {
            return Expression.class;
        }
    }
}
