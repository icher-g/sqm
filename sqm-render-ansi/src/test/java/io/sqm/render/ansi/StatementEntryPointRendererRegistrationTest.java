package io.sqm.render.ansi;

import io.sqm.core.DeleteStatement;
import io.sqm.core.Query;
import io.sqm.core.Statement;
import io.sqm.core.UpdateStatement;
import io.sqm.render.spi.RenderersRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class StatementEntryPointRendererRegistrationTest {

    @Test
    void ansiRegistryRegistersStatementAndQueryEntryPoints() {
        RenderersRepository repo = Renderers.ansi();

        assertInstanceOf(io.sqm.render.StatementRenderer.class, repo.require(Statement.class));
        assertInstanceOf(io.sqm.render.QueryRenderer.class, repo.require(Query.class));
        assertInstanceOf(UpdateStatementRenderer.class, repo.require(UpdateStatement.class));
        assertInstanceOf(DeleteStatementRenderer.class, repo.require(DeleteStatement.class));
    }
}