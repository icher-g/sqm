package io.sqm.parser.oracle;

import io.sqm.core.NavigationMode;
import io.sqm.core.Node;
import io.sqm.core.RuleMode;
import io.sqm.core.UniquenessMode;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.ParserException;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.Parser;

import java.util.List;

import static io.sqm.parser.core.TokenType.*;

/**
 * Shared small grammar operations for MODEL declarations, options, and lists.
 */
final class ModelGrammar {
    private ModelGrammar() {
    }

    /**
     * Checks capability before consuming any MODEL syntax.
     */
    static void require(Cursor cur, ParseContext ctx) {
        if (!ctx.capabilities().supports(SqlFeature.MODEL_CLAUSE)) {
            throw new ParserException("MODEL clause is not supported by this dialect", cur.fullPos());
        }
    }

    /**
     * Delegates a child fragment such as sales[2025] to its registered parser.
     */
    static <T extends Node> T node(Class<T> type, Cursor cur, ParseContext ctx) {
        var parsed = ctx.parse(type, cur);
        if (parsed.isError()) throw new ParserException(parsed.errorMessage(), cur.fullPos());
        return parsed.value();
    }

    /**
     * Parses nonempty lists such as (product, year) or [2025, 'Bike'] using parseItems.
     */
    static <T extends Node> List<T> list(Parser<?> owner, Class<T> type, Cursor cur, ParseContext ctx,
        TokenType open, TokenType close) {
        cur.expect("Expected " + open + " before MODEL list", open);
        var parsed = owner.<T, T>parseItems(type, cur, ctx);
        if (parsed.isError()) throw new ParserException(parsed.errorMessage(), cur.fullPos());
        cur.expect("Expected " + close + " after MODEL list", close);
        return parsed.value();
    }

    /**
     * Parses KEEP NAV UNIQUE DIMENSION or IGNORE NAV UNIQUE SINGLE REFERENCE, with either part optional.
     */
    static Options options(Cursor cur, Options defaults) {
        var navigation = defaults.navigation();
        var uniqueness = defaults.uniqueness();
        if (cur.match(KEEP) || cur.match(IGNORE)) {
            navigation = cur.advance().type() == KEEP ? NavigationMode.KEEP : NavigationMode.IGNORE;
            cur.expect("Expected NAV after KEEP or IGNORE", NAV);
        }
        if (cur.consumeIf(UNIQUE)) {
            if (cur.consumeIf(DIMENSION)) {
                uniqueness = UniquenessMode.DIMENSION;
            }
            else {
                cur.expect("Expected DIMENSION or SINGLE after UNIQUE", SINGLE);
                cur.expect("Expected REFERENCE after UNIQUE SINGLE", REFERENCE);
                uniqueness = UniquenessMode.SINGLE_REFERENCE;
            }
        }
        if (cur.match(KEEP) || cur.match(IGNORE) || cur.match(UNIQUE)) {
            throw new ParserException("Duplicate or reordered MODEL cell-reference options", cur.fullPos());
        }
        return new Options(navigation, uniqueness);
    }

    /**
     * Parses UPDATE, UPSERT, or UPSERT ALL; returns null when no mode is present.
     */
    static RuleMode mode(Cursor cur) {
        if (cur.consumeIf(UPDATE)) return RuleMode.UPDATE;
        if (cur.consumeIf(UPSERT)) return cur.consumeIf(ALL) ? RuleMode.UPSERT_ALL : RuleMode.UPSERT;
        return null;
    }

    /**
     * Effective options inherited by a main model before local overrides.
     */
    record Options(NavigationMode navigation, UniquenessMode uniqueness) {
        static final Options DEFAULT = new Options(NavigationMode.KEEP, UniquenessMode.DIMENSION);
    }
}
