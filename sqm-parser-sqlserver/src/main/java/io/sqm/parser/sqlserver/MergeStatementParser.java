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

        boolean matchedUpdate = false;
        boolean matchedDelete = false;
        boolean notMatchedInsert = false;

        for (var clause : clauses) {
            if (clause.matchType() == MergeClause.MatchType.MATCHED && clause.action() instanceof MergeUpdateAction) {
                if (matchedUpdate) {
                    return error("SQL Server MERGE supports at most one WHEN MATCHED THEN UPDATE clause in this slice", cur.fullPos());
                }
                matchedUpdate = true;
            }
            else if (clause.matchType() == MergeClause.MatchType.MATCHED && clause.action() instanceof MergeDeleteAction) {
                if (matchedDelete) {
                    return error("SQL Server MERGE supports at most one WHEN MATCHED THEN DELETE clause in this slice", cur.fullPos());
                }
                matchedDelete = true;
            }
            else if (clause.matchType() == MergeClause.MatchType.NOT_MATCHED && clause.action() instanceof MergeInsertAction) {
                if (notMatchedInsert) {
                    return error("SQL Server MERGE supports at most one WHEN NOT MATCHED THEN INSERT clause in this slice", cur.fullPos());
                }
                notMatchedInsert = true;
            }
        }
        return ok(List.copyOf(clauses));
    }
}
