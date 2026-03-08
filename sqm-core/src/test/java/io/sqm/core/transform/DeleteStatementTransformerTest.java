package io.sqm.core.transform;

import io.sqm.core.Node;
import io.sqm.core.Table;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.delete;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class DeleteStatementTransformerTest {

    @Test
    void preservesIdentityWhenChildrenDoNotChange() {
        var statement = delete(tbl("users")).build();

        Node transformed = new RecursiveNodeTransformer() {
        }.transform(statement);

        assertSame(statement, transformed);
    }

    @Test
    void rebuildsStatementWhenTargetChanges() {
        var statement = delete(tbl("users")).build();

        Node transformed = new RecursiveNodeTransformer() {
            @Override
            public Node visitTable(Table table) {
                return table.inSchema("public");
            }
        }.transform(statement);

        assertNotSame(statement, transformed);
    }
}
