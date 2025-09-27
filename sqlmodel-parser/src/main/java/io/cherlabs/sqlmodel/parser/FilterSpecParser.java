package io.cherlabs.sqlmodel.parser;

import io.cherlabs.sqlmodel.core.Filter;
import io.cherlabs.sqlmodel.parser.ast.Expr;
import io.cherlabs.sqlmodel.parser.ast.FilterVisitor;
import io.cherlabs.sqlmodel.parser.core.Cursor;
import io.cherlabs.sqlmodel.parser.expr.ExprParser;

public class FilterSpecParser implements SpecParser<Filter> {

    @Override
    public Class<Filter> targetType() {
        return Filter.class;
    }

    @Override
    public ParseResult<Filter> parse(Cursor cur) {
        try {
            Expr ast = new ExprParser(cur).parseExpr();
            Filter filter = toFilter(ast);
            return ParseResult.ok(filter);
        } catch (Exception e) {
            return ParseResult.error(e.getMessage());
        }
    }

    private Filter toFilter(Expr e) {
        return e.accept(new FilterVisitor());
    }
}
