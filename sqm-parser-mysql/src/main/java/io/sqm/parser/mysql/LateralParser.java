package io.sqm.parser.mysql;

import io.sqm.core.Lateral;
import io.sqm.core.QueryTable;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses MySQL {@code LATERAL} derived tables.
 *
 * <p>MySQL supports {@code LATERAL} only for derived tables, not for the
 * broader shared {@link io.sqm.core.TableRef} family. This parser therefore
 * accepts only {@link QueryTable} children and requires an alias.</p>
 */
public final class LateralParser extends io.sqm.parser.ansi.LateralParser {
    /**
     * Creates a MySQL lateral parser.
     */
    public LateralParser() {
    }

    /**
     * Parses MySQL {@code LATERAL} derived tables.
     *
     * @param cur token cursor.
     * @param ctx parse context.
     * @return parsed lateral derived table result.
     */
    @Override
    public ParseResult<? extends Lateral> parse(Cursor cur, ParseContext ctx) {
        if (!ctx.capabilities().supports(SqlFeature.LATERAL)) {
            return error("LATERAL is not supported by this dialect", cur.fullPos());
        }
        cur.expect("Expected LATERAL", TokenType.LATERAL);
        var queryTable = ctx.parse(QueryTable.class, cur);
        if (queryTable.isError()) {
            return error("MySQL LATERAL requires a derived table", cur.fullPos());
        }
        if (queryTable.value().alias() == null) {
            return error("MySQL LATERAL derived tables require an alias", cur.fullPos());
        }
        return ok(Lateral.of(queryTable.value()));
    }
}