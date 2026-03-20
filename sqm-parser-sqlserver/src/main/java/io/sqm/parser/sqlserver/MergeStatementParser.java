package io.sqm.parser.sqlserver;

import io.sqm.core.MergeClause;
import io.sqm.core.MergeDeleteAction;
import io.sqm.core.MergeInsertAction;
import io.sqm.core.MergeStatement;
import io.sqm.core.MergeUpdateAction;
import io.sqm.core.Predicate;
import io.sqm.core.ResultClause;
import io.sqm.core.Table;
import io.sqm.core.TableRef;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses the first SQL Server {@code MERGE} slice.
 */
public class MergeStatementParser extends io.sqm.parser.ansi.MergeStatementParser {

    /**
     * Creates a SQL Server merge-statement parser.
     */
    public MergeStatementParser() {
    }

    @Override
    public ParseResult<? extends MergeStatement> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected MERGE", TokenType.MERGE);
        if (!ctx.capabilities().supports(SqlFeature.MERGE_STATEMENT)) {
            return error("MERGE is not supported by this dialect", cur.fullPos());
        }
        if (cur.match(TokenType.TOP)) {
            return error("SQL Server MERGE TOP is not supported yet", cur.fullPos());
        }

        cur.consumeIf(TokenType.INTO);

        var target = ctx.parse(Table.class, cur);
        if (target.isError()) {
            return error(target);
        }

        cur.expect("Expected USING after MERGE target", TokenType.USING);
        var source = ctx.parse(TableRef.class, cur);
        if (source.isError()) {
            return error(source);
        }

        cur.expect("Expected ON after MERGE source", TokenType.ON);
        var on = ctx.parse(Predicate.class, cur);
        if (on.isError()) {
            return error(on);
        }

        var clauses = parseClauses(cur, ctx);
        if (clauses.isError()) {
            return error(clauses);
        }

        if (cur.match(TokenType.OUTPUT)) {
            return error("SQL Server MERGE OUTPUT is not supported yet", cur.fullPos());
        }
        if (cur.match(TokenType.WHEN)) {
            return error("Duplicate or unsupported SQL Server MERGE clause ordering", cur.fullPos());
        }

        ResultClause result = null;
        return ok(MergeStatement.of(target.value(), source.value(), on.value(), clauses.value(), result));
    }

    private ParseResult<List<MergeClause>> parseClauses(Cursor cur, ParseContext ctx) {
        var clauses = new ArrayList<MergeClause>();
        while (cur.match(TokenType.WHEN)) {
            var clause = ctx.parse(MergeClause.class, cur);
            if (clause.isError()) {
                return error(clause);
            }
            clauses.add(clause.value());
        }
        if (clauses.isEmpty()) {
            return error("Expected at least one MERGE clause", cur.fullPos());
        }

        var matchedClauses = clauses.stream()
            .filter(clause -> clause.matchType() == MergeClause.MatchType.MATCHED)
            .toList();
        if (matchedClauses.size() > 2) {
            return error("SQL Server MERGE supports at most two WHEN MATCHED clauses", cur.fullPos());
        }

        if (matchedClauses.size() == 2) {
            if (matchedClauses.getFirst().condition() == null) {
                return error("SQL Server MERGE requires the first WHEN MATCHED clause to include AND <search_condition> when two MATCHED clauses are present", cur.fullPos());
            }
            var firstAction = matchedClauses.getFirst().action();
            var secondAction = matchedClauses.get(1).action();
            if ((firstAction instanceof MergeUpdateAction && secondAction instanceof MergeUpdateAction)
                || (firstAction instanceof MergeDeleteAction && secondAction instanceof MergeDeleteAction)) {
                return error("SQL Server MERGE requires one UPDATE and one DELETE action when two WHEN MATCHED clauses are present", cur.fullPos());
            }
        }

        long notMatchedInsertCount = clauses.stream()
            .filter(clause -> clause.matchType() == MergeClause.MatchType.NOT_MATCHED && clause.action() instanceof MergeInsertAction)
            .count();
        if (notMatchedInsertCount > 1) {
            return error("SQL Server MERGE supports at most one WHEN NOT MATCHED THEN INSERT clause", cur.fullPos());
        }
        return ok(List.copyOf(clauses));
    }
}
