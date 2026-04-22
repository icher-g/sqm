package io.sqm.playground.rest.service;

import io.sqm.core.*;
import io.sqm.playground.api.AstChildSlotDto;
import io.sqm.playground.api.AstDetailDto;
import io.sqm.playground.api.AstNodeDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests reflective SQM AST mapping behavior.
 */
class SqmAstMapperTest {

    private static boolean hasSlot(AstNodeDto node, String slot) {
        return node.children().stream().anyMatch(child -> Objects.equals(slot, child.slot()));
    }

    private static AstChildSlotDto slot(AstNodeDto node, String slot) {
        return node.children().stream()
            .filter(child -> Objects.equals(slot, child.slot()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing AST slot: " + slot));
    }

    private static AstDetailDto detail(AstNodeDto node, String name) {
        return node.details().stream()
            .filter(detail -> Objects.equals(name, detail.name()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing AST detail: " + name));
    }

    private static boolean hasDetail(AstNodeDto node, String name) {
        return node.details().stream().anyMatch(detail -> Objects.equals(name, detail.name()));
    }

    @Test
    void mapsStructuralAccessorsIntoChildSlots() {
        var mapper = new SqmAstMapper();
        var customerId = ColumnExpr.of(Identifier.of("c"), Identifier.of("id"));
        var customerName = ColumnExpr.of(Identifier.of("c"), Identifier.of("name"));

        var query = Query.select(
                customerId.as("customerId"),
                customerName
            )
            .from(TableRef.table(Identifier.of("customer")).as("c"))
            .where(customerId.eq(Expression.literal(1)))
            .orderBy(customerName.desc())
            .build();

        var ast = mapper.toAst(query);

        assertEquals("SelectQuery", ast.nodeType());
        assertEquals("io.sqm.core.SelectQuery", ast.nodeInterface());
        assertEquals("statement", ast.category());
        assertNotNull(slot(ast, "items"));
        assertNotNull(slot(ast, "from"));
        assertNotNull(slot(ast, "where"));
        assertNotNull(slot(ast, "orderBy"));
        assertEquals("ExprSelectItem", slot(ast, "items").nodes().getFirst().nodeType());
        assertEquals("Table", slot(ast, "from").nodes().getFirst().nodeType());
        assertEquals(List.of("items", "from", "where", "orderBy"), ast.children().stream().map(AstChildSlotDto::slot).toList());
    }

    @Test
    void mapsStatementSequenceAsBatchCategory() {
        var mapper = new SqmAstMapper();

        var ast = mapper.toAst(StatementSequence.of(
            Query.select(Expression.literal(1)).build(),
            Query.select(Expression.literal(2)).build()
        ));

        assertEquals("StatementSequence", ast.nodeType());
        assertEquals("statementSequence", ast.category());
        assertEquals(2, slot(ast, "statements").nodes().size());
    }

    @Test
    void excludesDefaultHelperMethodsFromAstChildren() {
        var mapper = new SqmAstMapper();
        var column = ColumnExpr.of(Identifier.of("c"), Identifier.of("id"));

        var ast = mapper.toAst(column);

        assertEquals("ColumnExpr", ast.nodeType());
        assertTrue(ast.children().isEmpty());
        assertFalse(hasSlot(ast, "toSelectItem"));
        assertFalse(hasSlot(ast, "neg"));
        assertFalse(hasSlot(ast, "matchExpression"));
        assertEquals("c", detail(ast, "tableAlias").value());
        assertEquals("id", detail(ast, "name").value());
    }

    @Test
    void mapsIdentifierListsAsDetailsInsteadOfChildNodes() {
        var mapper = new SqmAstMapper();

        var subquery = Query.select(Expression.literal(1)).build();
        var queryTable = QueryTable.of(
            subquery,
            Identifier.of("q"),
            List.of(Identifier.of("id"), Identifier.of("value"))
        );

        var ast = mapper.toAst(queryTable);

        assertEquals("QueryTable", ast.nodeType());
        assertNotNull(slot(ast, "query"));
        assertFalse(hasSlot(ast, "columnAliases"));
        assertEquals("[id, value]", detail(ast, "columnAliases").value());
    }

    @Test
    void mapsEnumsWithoutImplementationMetadataDetails() {
        var mapper = new SqmAstMapper();
        var orderItem = ColumnExpr.of(Identifier.of("c"), Identifier.of("id")).desc().nullsLast();

        var ast = mapper.toAst(orderItem);

        assertEquals("orderItem", ast.category());
        assertFalse(hasDetail(ast, "interfaceSimpleName"));
        assertFalse(hasDetail(ast, "implementationClass"));
        assertEquals("DESC", detail(ast, "direction").value());
        assertEquals("LAST", detail(ast, "nulls").value());
        assertNotNull(slot(ast, "expr"));
    }

    @Test
    void excludesBuilderMethodsFromAstDetails() {
        var mapper = new SqmAstMapper();
        var query = Query.select(Expression.literal(1)).from(TableRef.table(Identifier.of("customer"))).build();

        var ast = mapper.toAst(query);

        assertFalse(hasDetail(ast, "builder"));
        assertFalse(hasSlot(ast, "builder"));
    }

    @Test
    void mapsQualifiedNamesLikeIdentifierCollections() {
        var mapper = new SqmAstMapper();
        var assignment = io.sqm.core.Assignment.of(
            QualifiedName.of(Identifier.of("o"), Identifier.of("status")),
            Expression.literal("priority")
        );

        var ast = mapper.toAst(assignment);

        assertEquals("o.status", detail(ast, "column").value());
    }

    @Test
    void rendersQuotedIdentifiersInline() {
        var mapper = new SqmAstMapper();
        var column = ColumnExpr.of(
            Identifier.of("Order", QuoteStyle.DOUBLE_QUOTE),
            Identifier.of("Line Item", QuoteStyle.BRACKETS)
        );

        var ast = mapper.toAst(column);

        assertEquals("\"Order\"", detail(ast, "tableAlias").value());
        assertEquals("[Line Item]", detail(ast, "name").value());
    }
}
