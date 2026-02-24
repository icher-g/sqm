package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OperatorNameTest {

    @Test
    void bareOperatorFactory() {
        var op = OperatorName.of("->");

        assertEquals("->", op.symbol());
        assertFalse(op.operatorKeywordSyntax());
        assertFalse(op.qualified());
        assertEquals("->", op.text());
    }

    @Test
    void postgresOperatorFactoryWithoutSchema() {
        var op = OperatorName.operator("+");

        assertTrue(op.operatorKeywordSyntax());
        assertFalse(op.qualified());
        assertEquals("OPERATOR(+)", op.text());
    }

    @Test
    void postgresOperatorFactoryWithSchema() {
        var op = OperatorName.operator(QualifiedName.of(Identifier.of("pg_catalog")), "##");

        assertTrue(op.operatorKeywordSyntax());
        assertTrue(op.qualified());
        assertEquals("OPERATOR(pg_catalog.##)", op.text());
    }

    @Test
    void blankSymbolRejected() {
        assertThrows(IllegalArgumentException.class, () -> OperatorName.of(" "));
    }

    @Test
    void schemaRequiresPostgresOperatorSyntax() {
        assertThrows(IllegalArgumentException.class, () ->
            new OperatorName(QualifiedName.of("pg_catalog"), "##", OperatorName.Syntax.BARE)
        );
    }
}
