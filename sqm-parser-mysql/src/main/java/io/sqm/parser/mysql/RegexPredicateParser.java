package io.sqm.parser.mysql;

import io.sqm.core.Expression;
import io.sqm.core.RegexMode;
import io.sqm.core.RegexPredicate;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.InfixParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parser for MySQL REGEXP/RLIKE predicates.
 */
public class RegexPredicateParser implements Parser<RegexPredicate>, InfixParser<Expression, RegexPredicate> {

    /**
     * Creates a MySQL regex-predicate parser.
     */
    public RegexPredicateParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends RegexPredicate> parse(Cursor cur, ParseContext ctx) {
        var value = ctx.parse(Expression.class, cur);
        if (value.isError()) {
            return error(value);
        }
        return parse(value.value(), cur, ctx);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends RegexPredicate> targetType() {
        return RegexPredicate.class;
    }

    /**
     * Parses a binary operator occurrence where the left-hand side operand
     * has already been parsed.
     *
     * @param lhs the already parsed left-hand operand
     * @param cur the cursor positioned at the operator token
     * @param ctx the parse context
     * @return the parsing result representing {@code lhs REGEXP rhs} or {@code lhs RLIKE rhs}
     */
    @Override
    public ParseResult<RegexPredicate> parse(Expression lhs, Cursor cur, ParseContext ctx) {
        if (!ctx.capabilities().supports(SqlFeature.REGEX_PREDICATE)) {
            return error("MySQL REGEXP/RLIKE predicates are not supported by this dialect", cur.fullPos());
        }

        boolean negated = cur.consumeIf(TokenType.NOT);

        var token = cur.expect("Expected REGEXP or RLIKE", TokenType.IDENT);
        if (!isMySqlRegexKeyword(token.lexeme())) {
            return error("Expected REGEXP or RLIKE", cur.fullPos());
        }

        var pattern = ctx.parse(Expression.class, cur);
        if (pattern.isError()) {
            return error(pattern);
        }

        return ok(RegexPredicate.of(RegexMode.MATCH, lhs, pattern.value(), negated));
    }

    private boolean isMySqlRegexKeyword(String lexeme) {
        return "REGEXP".equalsIgnoreCase(lexeme) || "RLIKE".equalsIgnoreCase(lexeme);
    }
}

