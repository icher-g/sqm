package io.sqm.parser.oracle;

import io.sqm.core.MergeClause;
import io.sqm.core.MergeStatement;
import io.sqm.core.Predicate;
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
 * Parses the baseline Oracle {@code MERGE} statement subset.
 */
public class MergeStatementParser extends io.sqm.parser.ansi.MergeStatementParser {

    /**
     * Creates an Oracle merge-statement parser.
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
            return error("Oracle MERGE does not support TOP", cur.fullPos());
        }

        cur.expect("Expected INTO after MERGE", TokenType.INTO);
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
        var on = ctx.parseEnclosed(Predicate.class, cur);
        if (on.isError()) {
            return error(on);
        }

        var clauses = parseClauses(cur, ctx);
        if (clauses.isError()) {
            return error(clauses);
        }

        if (cur.match(TokenType.RETURNING)) {
            return error("Oracle MERGE RETURNING is not supported by SQM", cur.fullPos());
        }
        if (cur.match(TokenType.OUTPUT)) {
            return error("MERGE ... OUTPUT is not supported by Oracle", cur.fullPos());
        }

        return ok(MergeStatement.of(target.value(), source.value(), on.value(), null, clauses.value(), null, List.of()));
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
        return ok(List.copyOf(clauses));
    }
}
