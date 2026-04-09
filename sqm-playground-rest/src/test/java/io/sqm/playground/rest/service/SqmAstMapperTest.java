package io.sqm.playground.rest.service;

import io.sqm.core.ColumnExpr;
import io.sqm.core.Expression;
import io.sqm.core.Identifier;
import io.sqm.core.OrderItem;
import io.sqm.core.Query;
import io.sqm.core.QueryTable;
import io.sqm.core.TableRef;
import io.sqm.playground.api.AstChildSlotDto;
import io.sqm.playground.api.AstDetailDto;
import io.sqm.playground.api.AstNodeDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests reflective SQM AST mapping behavior.
 */
class SqmAstMapperTest {

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
            .orderBy(OrderItem.of(customerName).desc())
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
        assertEquals("0", detail(ast, "columnAliasesQuotedCount").value());
    }

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
}
