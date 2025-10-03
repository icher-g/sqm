package io.cherlabs.sqlmodel.parser;

import io.cherlabs.sqlmodel.core.Filter;
import io.cherlabs.sqlmodel.parser.ast.Expr;
import io.cherlabs.sqlmodel.parser.ast.FilterVisitor;
import io.cherlabs.sqlmodel.parser.core.Cursor;
import io.cherlabs.sqlmodel.parser.expr.ExprParser;

/**
 * A spec parser for filter specifications.
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     "o.status IN ('A','B') AND d.id= 3"
 *     }
 * </pre>
 */
public class FilterSpecParser implements SpecParser<Filter> {

    /**
     * Gets the {@link Filter} type.
     *
     * @return {@link Filter} type.
     */
    @Override
    public Class<Filter> targetType() {
        return Filter.class;
    }

    /**
     * Parses the filter specification.
     *
     * @param cur the {@link Cursor} class containing the tokens.
     * @return a parser result.
     */
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
