package io.sqm.parser.ansi;

import io.sqm.core.Identifier;
import io.sqm.core.TableRef;
import io.sqm.core.UnpivotInput;
import io.sqm.core.UnpivotTable;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.InfixParser;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses relational {@code UNPIVOT (...)} table transforms.
 */
public class UnpivotTableParser implements MatchableParser<UnpivotTable>, InfixParser<TableRef, UnpivotTable> {
    /**
     * Creates an unpivot table parser.
     */
    public UnpivotTableParser() {
    }

    /**
     * Parses a standalone unpivot table.
     *
     * @param cur cursor positioned at {@code UNPIVOT}
     * @param ctx parse context
     * @return error because unpivot requires a left source relation
     */
    @Override
    public ParseResult<? extends UnpivotTable> parse(Cursor cur, ParseContext ctx) {
        return error("UNPIVOT requires a source table", cur.fullPos());
    }

    /**
     * Parses {@code source UNPIVOT (...)}.
     *
     * @param source source table reference
     * @param cur cursor positioned at {@code UNPIVOT}
     * @param ctx parse context
     * @return unpivot table
     */
    @Override
    public ParseResult<UnpivotTable> parse(TableRef source, Cursor cur, ParseContext ctx) {
        cur.expect("Expected UNPIVOT", TokenType.UNPIVOT);
        if (!ctx.capabilities().supports(SqlFeature.UNPIVOT_TABLE)) {
            return error("UNPIVOT is not supported by this dialect", cur.fullPos());
        }
        var nullTreatment = parseNullTreatment(cur);
        if (nullTreatment.isError()) {
            return error(nullTreatment);
        }
        cur.expect("Expected '(' after UNPIVOT", TokenType.LPAREN);
        var valueColumns = parseColumnGroup(cur);
        cur.expect("Expected FOR in UNPIVOT", TokenType.FOR);
        var nameColumn = toIdentifier(cur.expect("Expected UNPIVOT name column", TokenType.IDENT));
        cur.expect("Expected IN in UNPIVOT", TokenType.IN);
        cur.expect("Expected '(' after UNPIVOT IN", TokenType.LPAREN);
        var inputs = parseInputs(cur, ctx);
        if (inputs.isError()) {
            return error(inputs);
        }
        cur.expect("Expected ')' after UNPIVOT inputs", TokenType.RPAREN);
        cur.expect("Expected ')' after UNPIVOT", TokenType.RPAREN);

        return ok(UnpivotTable.of(source, valueColumns, nameColumn, inputs.value(), nullTreatment.value(), parseAliasIdentifier(cur)));
    }

    /**
     * Gets the parser target type.
     *
     * @return unpivot table type
     */
    @Override
    public Class<UnpivotTable> targetType() {
        return UnpivotTable.class;
    }

    /**
     * Checks whether the current token starts an unpivot transform.
     *
     * @param cur cursor
     * @param ctx parse context
     * @return {@code true} when current token is {@code UNPIVOT}
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.UNPIVOT);
    }

    /**
     * Parses optional null-row treatment.
     *
     * @param cur cursor positioned after {@code UNPIVOT}
     * @return parsed null treatment
     */
    protected ParseResult<UnpivotTable.NullTreatment> parseNullTreatment(Cursor cur) {
        if (cur.consumeIf(TokenType.INCLUDE)) {
            cur.expect("Expected NULLS after INCLUDE", TokenType.NULLS);
            return ok(UnpivotTable.NullTreatment.INCLUDE_NULLS);
        }
        if (cur.consumeIf(TokenType.EXCLUDE)) {
            cur.expect("Expected NULLS after EXCLUDE", TokenType.NULLS);
            return ok(UnpivotTable.NullTreatment.EXCLUDE_NULLS);
        }
        return ok(UnpivotTable.NullTreatment.DIALECT_DEFAULT);
    }

    private List<Identifier> parseColumnGroup(Cursor cur) {
        if (!cur.consumeIf(TokenType.LPAREN)) {
            return List.of(toIdentifier(cur.expect("Expected UNPIVOT value column", TokenType.IDENT)));
        }
        var columns = parseIdentifierItems(cur, "Expected UNPIVOT value column");
        cur.expect("Expected ')' after UNPIVOT value columns", TokenType.RPAREN);
        return columns;
    }

    private static ParseResult<List<UnpivotInput>> parseInputs(Cursor cur, ParseContext ctx) {
        List<UnpivotInput> inputs = new ArrayList<>();
        do {
            var input = ctx.parse(UnpivotInput.class, cur);
            if (input.isError()) {
                return error(input);
            }
            inputs.add(input.value());
        } while (cur.consumeIf(TokenType.COMMA));
        return ok(List.copyOf(inputs));
    }
}
