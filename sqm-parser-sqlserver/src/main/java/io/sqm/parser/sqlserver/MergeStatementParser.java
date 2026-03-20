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
import io.sqm.core.TopSpec;
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

        var topSpec = parseTopClause(cur, ctx);
        if (topSpec.isError()) {
            return error(topSpec);
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

        ResultClause result = null;

        if (cur.match(TokenType.OUTPUT)) {
            var parsedResult = ctx.parse(ResultClause.class, cur);
            if (parsedResult.isError()) {
                return error(parsedResult);
            }
            result = parsedResult.value();
        }

        if (cur.match(TokenType.WHEN)) {
            return error("Duplicate or unsupported SQL Server MERGE clause ordering", cur.fullPos());
        }

        return ok(MergeStatement.of(target.value(), source.value(), on.value(), topSpec.value(), clauses.value(), result));
    }

    private ParseResult<TopSpec> parseTopClause(Cursor cur, ParseContext ctx) {
        return SqlServerTopSpecParserSupport.parseTopClause(
            cur,
            ctx,
            false,
            "SQL Server MERGE TOP does not support WITH TIES"
        );
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
            var validation = validateDualUpdateDeleteClauses(matchedClauses, "WHEN MATCHED", cur);
            if (validation.isError()) {
                return validation;
            }
        }

        long notMatchedInsertCount = clauses.stream()
            .filter(clause -> clause.matchType() == MergeClause.MatchType.NOT_MATCHED && clause.action() instanceof MergeInsertAction)
            .count();

        if (notMatchedInsertCount > 1) {
            return error("SQL Server MERGE supports at most one WHEN NOT MATCHED THEN INSERT clause", cur.fullPos());
        }

        var notMatchedBySourceClauses = clauses.stream()
            .filter(clause -> clause.matchType() == MergeClause.MatchType.NOT_MATCHED_BY_SOURCE)
            .toList();

        if (notMatchedBySourceClauses.size() > 2) {
            return error("SQL Server MERGE supports at most two WHEN NOT MATCHED BY SOURCE clauses", cur.fullPos());
        }

        if (notMatchedBySourceClauses.size() == 2) {
            var validation = validateDualUpdateDeleteClauses(notMatchedBySourceClauses, "WHEN NOT MATCHED BY SOURCE", cur);
            if (validation.isError()) {
                return validation;
            }
        }
        return ok(List.copyOf(clauses));
    }

    private ParseResult<List<MergeClause>> validateDualUpdateDeleteClauses(List<MergeClause> clauses, String label, Cursor cur) {
        if (clauses.getFirst().condition() == null) {
            return error("SQL Server MERGE requires the first " + label + " clause to include AND <search_condition> when two "
                + label + " clauses are present", cur.fullPos());
        }

        var firstAction = clauses.getFirst().action();
        var secondAction = clauses.get(1).action();

        if ((firstAction instanceof MergeUpdateAction && secondAction instanceof MergeUpdateAction)
            || (firstAction instanceof MergeDeleteAction && secondAction instanceof MergeDeleteAction)) {
            return error("SQL Server MERGE requires one UPDATE and one DELETE action when two " + label + " clauses are present", cur.fullPos());
        }
        return ok(clauses);
    }
}
