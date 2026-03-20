package io.sqm.parser.postgresql;

import io.sqm.core.MergeClause;
import io.sqm.core.MergeStatement;
import io.sqm.core.Predicate;
import io.sqm.core.ResultClause;
import io.sqm.core.ResultItem;
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
 * Parses the first PostgreSQL {@code MERGE} slice.
 */
public class MergeStatementParser extends io.sqm.parser.ansi.MergeStatementParser {

    /**
     * Creates a PostgreSQL merge-statement parser.
     */
    public MergeStatementParser() {
    }

    @Override
    public ParseResult<? extends MergeStatement> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected MERGE", TokenType.MERGE);
        if (!ctx.capabilities().supports(SqlFeature.MERGE_STATEMENT)) {
            return error("MERGE is not supported by this PostgreSQL version", cur.fullPos());
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

        ResultClause result = null;
        if (cur.consumeIf(TokenType.RETURNING)) {
            if (!ctx.capabilities().supports(SqlFeature.DML_RESULT_CLAUSE)) {
                return error("MERGE ... RETURNING is not supported by this dialect", cur.fullPos());
            }
            var items = parseItems(ResultItem.class, cur, ctx);
            if (items.isError()) {
                return error(items);
            }
            result = ResultClause.of(items.value());
        }

        if (cur.match(TokenType.WHEN)) {
            return error("Duplicate or unsupported PostgreSQL MERGE clause ordering", cur.fullPos());
        }

        return ok(MergeStatement.of(target.value(), source.value(), on.value(), List.copyOf(clauses), result));
    }
}
