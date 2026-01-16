package io.sqm.core.repos;

import io.sqm.core.ColumnExpr;
import io.sqm.core.LiteralExpr;
import io.sqm.core.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link HandlersRepository}.
 * Tests the handlers repository pattern and registration/retrieval functionality.
 */
class HandlersRepositoryTest {

    private static class TestHandler<T extends Node> implements Handler<T> {
        private final Class<T> type;
        private final String name;

        public TestHandler(Class<T> type, String name) {
            this.type = type;
            this.name = name;
        }

        @Override
        public Class<? extends T> targetType() {
            return type;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class TestHandlersRepository implements HandlersRepository<Handler<?>> {
        private final java.util.Map<Class<?>, Handler<?>> handlers = new java.util.HashMap<>();

        @Override
        public <T extends Node> Handler<?> get(Class<T> type) {
            return handlers.get(type);
        }

        @Override
        public HandlersRepository<Handler<?>> register(Handler<?> handler) {
            // Store by the handler's target type
            handlers.put(handler.targetType(), handler);
            return this;
        }
    }

    @Test
    void registerAndRetrieveSingleHandler() {
        TestHandlersRepository repo = new TestHandlersRepository();
        TestHandler<ColumnExpr> columnHandler = new TestHandler<>(ColumnExpr.class, "ColumnHandler");

        repo.register(columnHandler);

        Handler<?> retrieved = repo.get(ColumnExpr.class);
        assertNotNull(retrieved);
        assertEquals("ColumnHandler", retrieved.toString());
    }

    @Test
    void registerAndRetrieveMultipleHandlers() {
        TestHandlersRepository repo = new TestHandlersRepository();
        TestHandler<ColumnExpr> columnHandler = new TestHandler<>(ColumnExpr.class, "ColumnHandler");
        TestHandler<LiteralExpr> literalHandler = new TestHandler<>(LiteralExpr.class, "LiteralHandler");

        repo.register(columnHandler).register(literalHandler);

        Handler<?> colRetrieved = repo.get(ColumnExpr.class);
        Handler<?> litRetrieved = repo.get(LiteralExpr.class);

        assertNotNull(colRetrieved);
        assertNotNull(litRetrieved);
        assertEquals("ColumnHandler", colRetrieved.toString());
        assertEquals("LiteralHandler", litRetrieved.toString());
    }

    @Test
    void handlerNotFound() {
        TestHandlersRepository repo = new TestHandlersRepository();
        Handler<?> handler = repo.get(ColumnExpr.class);
        assertNull(handler);
    }

    @Test
    void registerReturnsRepository() {
        TestHandlersRepository repo = new TestHandlersRepository();
        TestHandler<ColumnExpr> handler = new TestHandler<>(ColumnExpr.class, "Test");

        HandlersRepository<Handler<?>> result = repo.register(handler);
        assertSame(repo, result);
    }

    @Test
    void overwriteExistingHandler() {
        TestHandlersRepository repo = new TestHandlersRepository();
        TestHandler<ColumnExpr> handler1 = new TestHandler<>(ColumnExpr.class, "Handler1");
        TestHandler<ColumnExpr> handler2 = new TestHandler<>(ColumnExpr.class, "Handler2");

        repo.register(handler1);
        repo.register(handler2);

        Handler<?> retrieved = repo.get(ColumnExpr.class);
        assertEquals("Handler2", retrieved.toString());
    }

    @Test
    void requireHandlerSuccess() {
        TestHandlersRepository repo = new TestHandlersRepository();
        TestHandler<ColumnExpr> handler = new TestHandler<>(ColumnExpr.class, "ColumnHandler");
        repo.register(handler);

        Handler<?> required = repo.require(ColumnExpr.class);
        assertNotNull(required);
        assertEquals("ColumnHandler", required.toString());
    }

    @Test
    void requireHandlerThrowsWhenNotFound() {
        TestHandlersRepository repo = new TestHandlersRepository();
        assertThrows(IllegalArgumentException.class, () -> repo.require(ColumnExpr.class));
    }

    @Test
    void requireHandlerErrorMessage() {
        TestHandlersRepository repo = new TestHandlersRepository();
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> repo.require(ColumnExpr.class)
        );
        assertTrue(ex.getMessage().contains("No handler registered"));
        assertTrue(ex.getMessage().contains("ColumnExpr"));
    }

    @Test
    void chainedRegistration() {
        TestHandlersRepository repo = new TestHandlersRepository();
        TestHandler<ColumnExpr> h1 = new TestHandler<>(ColumnExpr.class, "H1");
        TestHandler<LiteralExpr> h2 = new TestHandler<>(LiteralExpr.class, "H2");

        repo.register(h1)
            .register(h2);

        assertEquals("H1", repo.get(ColumnExpr.class).toString());
        assertEquals("H2", repo.get(LiteralExpr.class).toString());
    }

    @Test
    void multipleRepositoriesIndependent() {
        TestHandlersRepository repo1 = new TestHandlersRepository();
        TestHandlersRepository repo2 = new TestHandlersRepository();

        TestHandler<ColumnExpr> h1 = new TestHandler<>(ColumnExpr.class, "Repo1Handler");
        TestHandler<ColumnExpr> h2 = new TestHandler<>(ColumnExpr.class, "Repo2Handler");

        repo1.register(h1);
        repo2.register(h2);

        assertEquals("Repo1Handler", repo1.get(ColumnExpr.class).toString());
        assertEquals("Repo2Handler", repo2.get(ColumnExpr.class).toString());
    }

    @Test
    void repositoryHandlesNullReturnWhenNotRegistered() {
        TestHandlersRepository repo = new TestHandlersRepository();
        Handler<?> handler = repo.get(ColumnExpr.class);
        assertNull(handler);
    }

    @Test
    void handlerInterfaceImplementation() {
        TestHandler<ColumnExpr> handler = new TestHandler<>(ColumnExpr.class, "Test");
        assertNotNull(handler);
        assertTrue(handler instanceof Handler);
    }

    @Test
    void registerMultipleHandlersForDifferentTypes() {
        TestHandlersRepository repo = new TestHandlersRepository();

        for (int i = 0; i < 5; i++) {
            TestHandler<ColumnExpr> handler = new TestHandler<>(ColumnExpr.class, "Handler" + i);
            repo.register(handler);
        }

        Handler<?> retrieved = repo.get(ColumnExpr.class);
        assertEquals("Handler4", retrieved.toString());
    }
}
