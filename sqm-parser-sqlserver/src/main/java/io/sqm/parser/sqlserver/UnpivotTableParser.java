package io.sqm.parser.sqlserver;

import io.sqm.core.UnpivotTable;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * SQL Server parser for the supported {@code UNPIVOT} table transform subset.
 */
public class UnpivotTableParser extends io.sqm.parser.ansi.UnpivotTableParser {
    /**
     * Creates a SQL Server unpivot table parser.
     */
    public UnpivotTableParser() {
    }

    @Override
    protected ParseResult<UnpivotTable.NullTreatment> parseNullTreatment(Cursor cur) {
        if (cur.match(TokenType.INCLUDE) || cur.match(TokenType.EXCLUDE)) {
            return error("SQL Server UNPIVOT null treatment is not supported", cur.fullPos());
        }
        return ok(UnpivotTable.NullTreatment.DIALECT_DEFAULT);
    }
}
