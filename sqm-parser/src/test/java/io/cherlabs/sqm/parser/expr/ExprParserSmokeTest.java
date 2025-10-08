package io.cherlabs.sqm.parser.expr;

import io.cherlabs.sqm.parser.ast.Expr;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Simple smoke tests that ExprParser can build an AST for common expressions. */
class ExprParserSmokeTest {

    @Test
    void parses_tuple_in_expression() {
        Expr e = new ExprParser("(a,b) IN ((1,2),(3,4))").parseExpr();
        assertNotNull(e);
        // No structural asserts here to avoid coupling; full behavior is tested in FilterSpecParser/FilterVisitor tests.
    }

    @Test
    void parses_boolean_composition() {
        Expr e = new ExprParser("x = 1 AND (y BETWEEN 2 AND 3 OR NOT z LIKE '%a%')").parseExpr();
        assertNotNull(e);
    }
}