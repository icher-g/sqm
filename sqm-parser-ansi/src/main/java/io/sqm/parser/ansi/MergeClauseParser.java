package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Baseline ANSI parser for {@link MergeClause}.
 */
public class MergeClauseParser implements Parser<MergeClause> {

    /**
     * Creates a merge-clause parser.
     */
    public MergeClauseParser() {
    }

    @Override
    public ParseResult<? extends MergeClause> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected WHEN", TokenType.WHEN);
        return error("MERGE clauses are not supported by this dialect", cur.fullPos());
    }

    /**
     * Parses the shared MERGE-clause subset used by dialect-specific implementations.
     *
     * @param cur parser cursor positioned at {@code WHEN}
     * @param ctx parse context
     * @return parsed merge clause or an error result
     */
    protected final ParseResult<? extends MergeClause> parseSupportedClause(Cursor cur, ParseContext ctx) {
        cur.expect("Expected WHEN", TokenType.WHEN);

        MergeClause.MatchType matchType;

        if (cur.consumeIf(TokenType.NOT)) {
            cur.expect("Expected MATCHED after WHEN NOT", TokenType.MATCHED);
            if (cur.consumeIf(TokenType.BY)) {
                matchType = parseNotMatchedBy(cur);
                if (matchType == null) {
                    return error(notMatchedByUnsupportedMessage(), cur.fullPos());
                }
            }
            else {
                matchType = MergeClause.MatchType.NOT_MATCHED;
            }
        }
        else {
            cur.expect("Expected MATCHED after WHEN", TokenType.MATCHED);
            matchType = MergeClause.MatchType.MATCHED;
        }

        Predicate condition = null;

        if (cur.consumeIf(TokenType.AND)) {
            var parsedCondition = ctx.parse(Predicate.class, cur);
            if (parsedCondition.isError()) {
                return error(parsedCondition);
            }
            condition = parsedCondition.value();
        }
        cur.expect("Expected THEN after MERGE match branch", TokenType.THEN);

        var action = ctx.parse(MergeAction.class, cur);
        if (action.isError()) {
            return error(action);
        }

        if ((matchType == MergeClause.MatchType.MATCHED || matchType == MergeClause.MatchType.NOT_MATCHED_BY_SOURCE)
            && action.value() instanceof MergeInsertAction) {
            return error("WHEN " + renderMatchType(matchType) + " clauses cannot use INSERT actions", cur.fullPos());
        }

        if (matchType == MergeClause.MatchType.NOT_MATCHED
            && !(action.value() instanceof MergeInsertAction)
            && !(action.value() instanceof MergeDoNothingAction)) {
            return error("WHEN NOT MATCHED clauses must use INSERT or DO NOTHING actions", cur.fullPos());
        }

        if ((matchType == MergeClause.MatchType.MATCHED || matchType == MergeClause.MatchType.NOT_MATCHED_BY_SOURCE)
            && !(action.value() instanceof MergeUpdateAction
            || action.value() instanceof MergeDeleteAction
            || action.value() instanceof MergeDoNothingAction)) {
            return error("WHEN " + renderMatchType(matchType) + " clauses must use UPDATE, DELETE, or DO NOTHING actions", cur.fullPos());
        }

        return ok(MergeClause.of(matchType, condition, action.value()));
    }

    /**
     * Parses the branch target after {@code WHEN NOT MATCHED BY}.
     *
     * @param cur parser cursor positioned after {@code BY}
     * @return resolved match type, or {@code null} when unsupported
     */
    protected MergeClause.MatchType parseNotMatchedBy(Cursor cur) {
        return null;
    }

    /**
     * Returns the diagnostic used when {@code WHEN NOT MATCHED BY ...} is unsupported.
     *
     * @return unsupported-feature diagnostic
     */
    protected String notMatchedByUnsupportedMessage() {
        return "MERGE ... WHEN NOT MATCHED BY ... is not supported by this dialect";
    }

    /**
     * Renders a merge match type for diagnostics.
     *
     * @param matchType match type to describe
     * @return user-facing match-type text
     */
    protected final String renderMatchType(MergeClause.MatchType matchType) {
        return switch (matchType) {
            case MATCHED -> "MATCHED";
            case NOT_MATCHED -> "NOT MATCHED";
            case NOT_MATCHED_BY_SOURCE -> "NOT MATCHED BY SOURCE";
        };
    }

    @Override
    public Class<MergeClause> targetType() {
        return MergeClause.class;
    }
}
