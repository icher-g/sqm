package io.sqm.parser;

import io.sqm.core.OrPredicate;
import io.sqm.core.Predicate;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Entry-point parser for SQL {@link Predicate} expressions.
 *
 * <p>This parser does not itself implement predicate logic. Instead, it
 * delegates to the {@link OrPredicate} parser, which represents the lowest
 * precedence level of boolean expressions (handling {@code OR} chains). Higher
 * precedence layers (such as {@code AND} and atomic predicate forms) are
 * handled by their respective parsers, which are transitively invoked by
 * {@code OrPredicateParser}.</p>
 *
 * <p>This structure mirrors a standard SQL boolean-expression grammar:</p>
 *
 * <pre>{@code
 * Predicate       → OrPredicate
 * OrPredicate     → AndPredicate ('OR' AndPredicate)*
 * AndPredicate    → AtomicPredicate ('AND' AtomicPredicate)*
 * AtomicPredicate → grouped | NOT predicate | EXISTS | comparisons | IN | BETWEEN | LIKE | IS ...
 * }</pre>
 *
 * <p>By delegating directly to {@code OrPredicate}, this class serves as the
 * canonical entry point for predicate parsing, providing a clean separation
 * between:
 *
 * <ul>
 *   <li>the <strong>public grammar entry</strong> ({@code Predicate}), and</li>
 *   <li>the <strong>precedence-aware implementation</strong> (OR → AND → atomic)</li>
 * </ul>
 *
 * <p>After delegation, the result is passed through {@link ParseContext#finalize} to
 * enforce end-of-rule checks and proper cursor alignment.</p>
 */
public class PredicateParser implements Parser<Predicate> {

    /**
     * Parses a SQL {@link Predicate} by delegating to the {@link OrPredicate}
     * parser, which handles full predicate precedence (OR, AND, atomic forms).
     *
     * @param cur the token cursor
     * @param ctx parsing context used for recursive dispatch and error handling
     * @return the parsed predicate, or an error result if parsing fails
     */
    @Override
    public ParseResult<? extends Predicate> parse(Cursor cur, ParseContext ctx) {
        return ctx.parse(OrPredicate.class, cur);
    }

    /**
     * Returns the model type produced by this parser — {@link Predicate}.
     */
    @Override
    public Class<Predicate> targetType() {
        return Predicate.class;
    }
}
