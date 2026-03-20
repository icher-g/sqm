package io.sqm.parser.sqlserver;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses SQL Server {@code WHEN ... THEN ...} MERGE clauses.
 */
public class MergeClauseParser extends io.sqm.parser.ansi.MergeClauseParser {

    /**
     * Creates a SQL Server merge-clause parser.
     */
    public MergeClauseParser() {
    }

    @Override
    public ParseResult<? extends MergeClause> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected WHEN", TokenType.WHEN);

        MergeClause.MatchType matchType;
        if (cur.consumeIf(TokenType.NOT)) {
            cur.expect("Expected MATCHED after WHEN NOT", TokenType.MATCHED);
            if (cur.match(TokenType.BY)) {
                return error("SQL Server MERGE ... WHEN NOT MATCHED BY ... is not supported yet", cur.fullPos());
            }
            matchType = MergeClause.MatchType.NOT_MATCHED;
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

        if (matchType == MergeClause.MatchType.MATCHED && action.value() instanceof MergeInsertAction) {
            return error("WHEN MATCHED clauses cannot use INSERT actions", cur.fullPos());
        }
        if (matchType == MergeClause.MatchType.NOT_MATCHED && !(action.value() instanceof MergeInsertAction)) {
            return error("WHEN NOT MATCHED clauses must use INSERT actions", cur.fullPos());
        }
        if (matchType == MergeClause.MatchType.MATCHED && !(action.value() instanceof MergeUpdateAction || action.value() instanceof MergeDeleteAction)) {
            return error("WHEN MATCHED clauses must use UPDATE or DELETE actions", cur.fullPos());
        }

        return ok(MergeClause.of(matchType, condition, action.value()));
    }
}
