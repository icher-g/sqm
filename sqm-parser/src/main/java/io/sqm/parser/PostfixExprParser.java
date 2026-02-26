package io.sqm.parser;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.MatchResult;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses expressions that can be followed by one or more postfix constructs.
 *
 * <p>This parser delegates the initial (base) expression parsing to an
 * {@link AtomicExprParser}, and then repeatedly applies postfix rules that
 * transform an already parsed left-hand-side (LHS) expression.</p>
 *
 * <p>Postfix parsing is implemented as a loop: after a base expression is parsed,
 * the parser tries to match known postfix constructs at the current cursor
 * position. When a postfix construct matches, it produces a new expression node
 * that becomes the new LHS, and the loop continues. The loop stops when no
 * postfix construct matches.</p>
 *
 * <h2>Supported postfix constructs</h2>
 * <ul>
 *   <li><b>Type casts</b> (e.g. {@code expr::type}, {@code a::int::text}) producing nested {@link CastExpr}.</li>
 *   <li><b>Array subscripts</b> (e.g. {@code arr[1][2]}) producing nested {@link ArraySubscriptExpr}.</li>
 *   <li><b>Array slices</b> (e.g. {@code arr[1:5]}, {@code arr[:5]}, {@code arr[1:]}, {@code arr[:]})
 *       producing nested {@link ArraySliceExpr}.</li>
 * </ul>
 *
 * <p>Postfix constructs are applied with high precedence: they bind to the
 * nearest expression on their left. For example, {@code a + b::int} is parsed as
 * {@code a + (b::int)} rather than {@code (a + b)::int}.</p>
 *
 * <p>Matching is delegated to the {@link ParseContext} via
 * {@link ParseContext#parseIfMatch(Class, Node, Cursor)}. Each postfix parser
 * decides whether it applies at the current cursor position and, if so, returns
 * the transformed result.</p>
 */
public class PostfixExprParser {

    private final AtomicExprParser atomicParser;

    /**
     * Creates a postfix expression parser.
     *
     * @param atomicParser parser for the base (atomic) expression part.
     */
    public PostfixExprParser(AtomicExprParser atomicParser) {
        this.atomicParser = atomicParser;
    }

    /**
     * Parses an expression starting with an atomic expression and then applies
     * any number of postfix constructs.
     *
     * <p>If no postfix construct matches after the base expression, the base
     * expression is returned unchanged.</p>
     *
     * @param cur the cursor positioned at the start of an expression
     * @param ctx the parse context
     * @return a parsed expression result (success or error)
     */
    public ParseResult<? extends Expression> parse(Cursor cur, ParseContext ctx) {
        // parse base expression
        var left = atomicParser.parse(cur, ctx);
        if (left.isError()) {
            return left;
        }

        // parse postfix operators
        while (true) {
            // support of a::int::text
            MatchResult<? extends Expression> matched = ctx.parseIfMatch(CastExpr.class, left.value(), cur);
            if (matched.match()) {
                left = matched.result();
                if (left.isError()) return left;
                continue;
            }

            // support of arr[1][2]
            matched = ctx.parseIfMatch(ArraySubscriptExpr.class, left.value(), cur);
            if (matched.match()) {
                left = matched.result();
                if (left.isError()) return left;
                continue;
            }

            // support of arr[1:5], arr[:5], arr[1:] or arr[:]
            matched = ctx.parseIfMatch(ArraySliceExpr.class, left.value(), cur);
            if (matched.match()) {
                left = matched.result();
                if (left.isError()) return left;
                continue;
            }

            // support of expr COLLATE collation
            matched = ctx.parseIfMatch(CollateExpr.class, left.value(), cur);
            if (matched.match()) {
                left = matched.result();
                if (left.isError()) return left;
                continue;
            }

            // support of PostgreSQL AT TIME ZONE
            matched = ctx.parseIfMatch(AtTimeZoneExpr.class, left.value(), cur);
            if (matched.match()) {
                left = matched.result();
                if (left.isError()) return left;
                continue;
            }
            break;
        }

        return ok(left.value());
    }
}
