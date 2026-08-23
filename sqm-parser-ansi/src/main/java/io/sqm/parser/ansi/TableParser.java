package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses table references.
 */
public class TableParser implements MatchableParser<Table> {
    /**
     * Creates a table parser.
     */
    public TableParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<Table> parse(Cursor cur, ParseContext ctx) {
        Table.Inheritance inheritance = Table.Inheritance.DEFAULT;

        if (cur.consumeIf(TokenType.ONLY)) {
            if (!ctx.capabilities().supports(SqlFeature.TABLE_INHERITANCE_ONLY)) {
                return error("ONLY is not supported by this dialect", cur.fullPos());
            }
            inheritance = Table.Inheritance.ONLY;
        }

        var t = cur.expect("Expected identifier", TokenType.IDENT);

        List<Identifier> parts = new ArrayList<>();
        parts.add(toIdentifier(t));

        while (cur.consumeIf(TokenType.DOT)) {
            t = cur.expect("Expected identifier after '.'", TokenType.IDENT);
            parts.add(toIdentifier(t));
        }

        if (cur.match(TokenType.OPERATOR) && "*".equals(cur.peek().lexeme())) {
            if (!ctx.capabilities().supports(SqlFeature.TABLE_INHERITANCE_DESCENDANTS)) {
                return error("Table inheritance '*' is not supported by this dialect", cur.fullPos());
            }
            if (inheritance == Table.Inheritance.ONLY) {
                return error("ONLY cannot be combined with inheritance '*'", cur.fullPos());
            }
            cur.advance();
            inheritance = Table.Inheritance.INCLUDE_DESCENDANTS;
        }

        Identifier name = parts.getLast();
        Identifier schema = null;
        if (parts.size() == 2) {
            schema = parts.getFirst();
        }
        else
            if (parts.size() > 2) {
                schema = Identifier.of(String.join(".", parts.subList(0, parts.size() - 1).stream().map(Identifier::value).toList()));
            }

        return parseAfterQualifiedName(cur, ctx, schema, name, inheritance);
    }

    /**
     * Parses table suffix tokens after base table name is resolved.
     *
     * @param cur         token cursor.
     * @param ctx         parse context.
     * @param schema      resolved table schema, or {@code null}.
     * @param name        resolved table name.
     * @param inheritance resolved inheritance mode.
     * @return parsing result.
     */
    protected ParseResult<Table> parseAfterQualifiedName(
        Cursor cur,
        ParseContext ctx,
        Identifier schema,
        Identifier name,
        Table.Inheritance inheritance) {
        TableVersionSpec version = null;
        TablePartitionSpec partitionSpec = null;

        if (isVersionSyntax(cur)) {
            if (!ctx.capabilities().supports(SqlFeature.TABLE_VERSIONING)) {
                return error("Table versioning is not supported by this dialect", cur.fullPos());
            }
            var parsedVersion = parseTableVersion(cur, ctx);
            if (parsedVersion.isError()) {
                return error(parsedVersion);
            }
            version = parsedVersion.value();
        }

        if (cur.match(TokenType.PARTITION) || cur.match(TokenType.SUBPARTITION)) {
            if (!ctx.capabilities().supports(SqlFeature.TABLE_PARTITION_SPEC)) {
                return error("Table partition specifications are not supported by this dialect", cur.fullPos());
            }
            var parsedSelector = parsePartitionSpec(cur);
            if (parsedSelector.isError()) {
                return error(parsedSelector);
            }
            partitionSpec = parsedSelector.value();
        }

        Identifier alias = parseAliasIdentifier(cur);
        return ok(Table.of(schema, name, alias, inheritance, List.of(), version, partitionSpec));
    }

    /**
     * Checks if the cursor is currently on a statement that looks like a version syntax.
     *
     * @param cur a cursor.
     * @return true if the cursor is currently positioned on a version and false otherwise.
     */
    protected boolean isVersionSyntax(Cursor cur) {
        return (cur.match(TokenType.AS) && cur.match(TokenType.OF, 1))
            || (cur.match(TokenType.FOR) && cur.match(TokenType.SYSTEM_TIME, 1));
    }

    /**
     * Parses a table version.
     *
     * @param cur a cursor.
     * @param ctx a context.
     * @return a parsed version specification.
     */
    protected ParseResult<TableVersionSpec> parseTableVersion(Cursor cur, ParseContext ctx) {
        if (cur.consumeIf(TokenType.AS)) {
            cur.expect("Expected OF after AS", TokenType.OF);
            if (cur.consumeIf(TokenType.SCN)) {
                var value = ctx.parse(Expression.class, cur);
                if (value.isError()) {
                    return error(value);
                }
                return ok(TableVersionSpec.of(TableVersionSpec.TableVersionKind.AS_OF_SCN, value.value()));
            }
            if (cur.match(TokenType.IDENT, "TIMESTAMP")) {
                cur.advance();
                var value = ctx.parse(Expression.class, cur);
                if (value.isError()) {
                    return error(value);
                }
                return ok(TableVersionSpec.of(TableVersionSpec.TableVersionKind.AS_OF_TIMESTAMP, value.value()));
            }
            return error("Expected SCN or TIMESTAMP after AS OF", cur.fullPos());
        }

        cur.expect("Expected FOR", TokenType.FOR);
        cur.expect("Expected SYSTEM_TIME after FOR", TokenType.SYSTEM_TIME);
        if (cur.consumeIf(TokenType.AS)) {
            cur.expect("Expected OF after AS", TokenType.OF);
            var value = ctx.parse(Expression.class, cur);
            if (value.isError()) {
                return error(value);
            }
            return ok(TableVersionSpec.of(TableVersionSpec.TableVersionKind.AS_OF_TIMESTAMP, value.value()));
        }
        if (cur.consumeIf(TokenType.FROM)) {
            var start = ctx.parse(Expression.class, cur);
            if (start.isError()) {
                return error(start);
            }
            cur.expect("Expected TO in FOR SYSTEM_TIME FROM", TokenType.TO);
            var end = ctx.parse(Expression.class, cur);
            if (end.isError()) {
                return error(end);
            }
            return ok(TableVersionSpec.range(TableVersionSpec.TableVersionKind.FROM_TO, start.value(), end.value()));
        }
        if (cur.consumeIf(TokenType.BETWEEN)) {
            var start = ctx.parse(Expression.class, cur);
            if (start.isError()) {
                return error(start);
            }
            cur.expect("Expected AND in FOR SYSTEM_TIME BETWEEN", TokenType.AND);
            var end = ctx.parse(Expression.class, cur);
            if (end.isError()) {
                return error(end);
            }
            return ok(TableVersionSpec.range(TableVersionSpec.TableVersionKind.BETWEEN, start.value(), end.value()));
        }
        if (cur.consumeIf(TokenType.CONTAINED)) {
            cur.expect("Expected IN after CONTAINED", TokenType.IN);
            cur.expect("Expected '(' after CONTAINED IN", TokenType.LPAREN);
            var start = ctx.parse(Expression.class, cur);
            if (start.isError()) {
                return error(start);
            }
            cur.expect("Expected comma in CONTAINED IN", TokenType.COMMA);
            var end = ctx.parse(Expression.class, cur);
            if (end.isError()) {
                return error(end);
            }
            cur.expect("Expected ')' after CONTAINED IN", TokenType.RPAREN);
            return ok(TableVersionSpec.range(TableVersionSpec.TableVersionKind.CONTAINED_IN, start.value(), end.value()));
        }
        if (cur.consumeIf(TokenType.ALL)) {
            return ok(TableVersionSpec.all());
        }
        return error("Expected SYSTEM_TIME selector", cur.fullPos());
    }

    /**
     * Parses partition specification.
     *
     * @param cur a cursor.
     * @return a parsed partition specification.
     */
    protected ParseResult<TablePartitionSpec> parsePartitionSpec(Cursor cur) {
        boolean subpartition = cur.consumeIf(TokenType.SUBPARTITION);
        if (!subpartition) {
            cur.expect("Expected PARTITION", TokenType.PARTITION);
        }
        cur.expect("Expected '(' after partition specification", TokenType.LPAREN);
        var names = parseIdentifierItems(cur, "Expected partition name");
        cur.expect("Expected ')' after partition specification", TokenType.RPAREN);
        return ok(subpartition ? TablePartitionSpec.subpartition(names) : TablePartitionSpec.partition(names));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<Table> targetType() {
        return Table.class;
    }

    /**
     * Performs a look-ahead test to determine whether this parser is applicable
     * at the current cursor position.
     * <p>
     * Implementations must <strong>not</strong> advance the cursor or modify
     * the {@link ParseContext}. Their sole responsibility is to inspect the
     * upcoming tokens and decide if this parser is responsible for them.
     *
     * @param cur the cursor pointing at the current token
     * @param ctx the parsing context providing configuration and utilities
     * @return {@code true} if this parser should be used to parse the upcoming
     * input, {@code false} otherwise
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        if (cur.match(TokenType.ONLY)) {
            return cur.match(TokenType.IDENT, 1);
        }
        if (cur.match(TokenType.IDENT)) {
            int i = 1;
            while (cur.match(TokenType.DOT, i)) {
                if (!cur.match(TokenType.IDENT, i + 1)) {
                    return false;
                }
                i += 2;
            }
            return true;
        }
        return false;
    }
}
