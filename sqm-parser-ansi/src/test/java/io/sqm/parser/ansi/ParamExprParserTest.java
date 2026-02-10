package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.ParserException;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParamExprParserTest {

    private SelectQuery parseSelect(String sql) {
        var ctx = ParseContext.of(new AnsiSpecs());
        var pr = ctx.parse(Query.class, sql);
        assertTrue(pr.ok());
        assertInstanceOf(SelectQuery.class, pr.value(), "Expected SelectQuery");
        return (SelectQuery) pr.value();
    }

    private void parseError(String sql) {
        var ctx = ParseContext.of(new AnsiSpecs());
        var pr = ctx.parse(Query.class, sql);
        if (pr.isError()) {
            throw new ParserException(pr.errorMessage(), pr.problems().getFirst().pos());
        }
    }

    private ComparisonPredicate extractSingleWhereComparison(SelectQuery q) {
        assertNotNull(q.where(), "Expected WHERE clause");
        assertInstanceOf(ComparisonPredicate.class, q.where(), "Test assumes a simple comparison predicate in WHERE");
        return (ComparisonPredicate) q.where();
    }

    @Test
    void parsesPositionalParameterAsPositionalExpr() {
        SelectQuery q = parseSelect("SELECT * FROM t WHERE a = $1");
        ComparisonPredicate where = extractSingleWhereComparison(q);

        Expression right = where.rhs();
        assertInstanceOf(OrdinalParamExpr.class, right);

        OrdinalParamExpr p = (OrdinalParamExpr) right;
        assertEquals(1, p.index());
    }

    @Test
    void notAlignedNumberToDollarShouldProduceParsingError() {
        assertThrows(ParserException.class, () -> parseError("SELECT * FROM t WHERE a = $ 1"));
    }

    @Test
    void notAlignedParameterNameToColonShouldProduceParsingError() {
        assertThrows(ParserException.class, () -> parseError("SELECT * FROM t WHERE a = : name"));
    }

    @Test
    void parsesNamedParameterWithColonAsNamedExpr() {
        SelectQuery q = parseSelect("SELECT * FROM t WHERE a = :id");
        ComparisonPredicate where = extractSingleWhereComparison(q);

        Expression right = where.rhs();
        assertInstanceOf(NamedParamExpr.class, right);

        NamedParamExpr p = (NamedParamExpr) right;
        assertEquals("id", p.name());
    }

    @Test
    void parsesNamedParameterBeforeRightParenInFunctionCall() {
        SelectQuery q = parseSelect("SELECT coalesce(:name, 'n/a') FROM t");
        assertEquals(1, q.items().size());
        assertInstanceOf(ExprSelectItem.class, q.items().getFirst());
        Expression expr = ((ExprSelectItem) q.items().getFirst()).expr();
        assertInstanceOf(FunctionExpr.class, expr);
        FunctionExpr fn = (FunctionExpr) expr;
        assertEquals("coalesce", fn.name());
        assertInstanceOf(FunctionExpr.Arg.ExprArg.class, fn.args().getFirst());
        Expression argExpr = ((FunctionExpr.Arg.ExprArg) fn.args().getFirst()).expr();
        assertInstanceOf(NamedParamExpr.class, argExpr);
        assertEquals("name", ((NamedParamExpr) argExpr).name());
    }

    @Test
    void parsesNamedParameterBeforeRightParenInInList() {
        SelectQuery q = parseSelect("SELECT * FROM t WHERE a IN (:kind_a, :kind_b)");
        assertInstanceOf(InPredicate.class, q.where());
        InPredicate in = (InPredicate) q.where();
        assertInstanceOf(RowExpr.class, in.rhs());
        RowExpr row = (RowExpr) in.rhs();
        assertEquals(2, row.items().size());
        assertInstanceOf(NamedParamExpr.class, row.items().get(0));
        assertInstanceOf(NamedParamExpr.class, row.items().get(1));
        assertEquals("kind_a", ((NamedParamExpr) row.items().get(0)).name());
        assertEquals("kind_b", ((NamedParamExpr) row.items().get(1)).name());
    }

    @Test
    void parseNamedParameterWithAtShouldFail() {
        assertThrows(ParserException.class, () -> parseError("SELECT * FROM t WHERE a = @user_id"));
    }

    @Test
    void parsesQuestionMarkParameterAsPositionalWithImplicitIndex() {
        // Depending on your design, you might auto-assign indices
        // in the order of appearance: ?, ?, ? => 1, 2, 3
        SelectQuery q = parseSelect("SELECT * FROM t WHERE a = ? AND b = ?");

        // you probably have a CompositePredicate(AND) here; adapt as needed
        assertInstanceOf(AndPredicate.class, q.where());
        AndPredicate and = (AndPredicate) q.where();
        assertInstanceOf(AnonymousParamExpr.class, ((ComparisonPredicate) and.lhs()).rhs());
        assertInstanceOf(AnonymousParamExpr.class, ((ComparisonPredicate) and.rhs()).rhs());
    }

    @Test
    void spacedOutSequencesAreNotParsedAsParameters() {
        // `$ 1` and `: id` should fail parsing or at least not become ParameterExpr
        // This test assumes the parser still builds an AST; adapt if you instead
        // throw a parse exception.
        var ctx = ParseContext.of(new AnsiSpecs());
        var pr = ctx.parse(Query.class, "SELECT * FROM t WHERE a = $ 1");
        assertFalse(pr.ok());
    }

    @Test
    void namedParamParserMatch_ignoresArraySliceSeparator() {
        var parser = new NamedParamExprParser();
        var ctx = ParseContext.of(new AnsiSpecs());

        assertFalse(parser.match(Cursor.of(":end]", ctx.identifierQuoting()), ctx));
        assertTrue(parser.match(Cursor.of(":name)", ctx.identifierQuoting()), ctx));
    }
}
