package io.sqm.core.transform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class TableSchemaResolverTest {

    @Test
    void no_op_resolver_returns_unresolved() {
        var result = TableSchemaResolver.NO_OP.resolve("users");
        assertInstanceOf(TableQualification.Unresolved.class, result);
    }
}
