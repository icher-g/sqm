package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.Identifier;
import io.sqm.core.JsonPathSpec;
import io.sqm.core.JsonTableBehavior;
import io.sqm.core.JsonTableColumn;
import io.sqm.core.JsonTableExistsColumn;
import io.sqm.core.JsonTableNestedPathColumn;
import io.sqm.core.JsonTableOrdinalityColumn;
import io.sqm.core.JsonTableRef;
import io.sqm.core.JsonTableScalarColumn;
import io.sqm.core.TypeName;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.ParserException;
import io.sqm.parser.core.Token;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses SQL/JSON {@code JSON_TABLE} table references.
 */
public class JsonTableRefParser implements MatchableParser<JsonTableRef> {
    /**
     * Creates a JSON table-reference parser.
     */
    public JsonTableRefParser() {
    }

    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.JSON_TABLE);
    }

    @Override
    public ParseResult<? extends JsonTableRef> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected JSON_TABLE", TokenType.JSON_TABLE);
        if (!ctx.capabilities().supports(SqlFeature.JSON_TABLE)) {
            return error("JSON_TABLE is not supported by this dialect", cur.fullPos());
        }

        cur.expect("Expected '(' after JSON_TABLE", TokenType.LPAREN);
        var json = ctx.parse(Expression.class, cur);
        if (json.isError()) {
            return error(json);
        }
        cur.expect("Expected comma after JSON_TABLE JSON expression", TokenType.COMMA);
        var rootPath = parsePathLiteral(cur, "Expected JSON_TABLE root path");
        expectKeyword(cur, "Expected COLUMNS in JSON_TABLE", "COLUMNS", TokenType.COLUMNS);
        cur.expect("Expected '(' after JSON_TABLE COLUMNS", TokenType.LPAREN);
        var columns = parseColumns(cur, ctx);
        cur.expect("Expected ')' after JSON_TABLE COLUMNS", TokenType.RPAREN);
        cur.expect("Expected ')' after JSON_TABLE", TokenType.RPAREN);

        Identifier alias = parseAliasIdentifier(cur);
        return ok(JsonTableRef.of(json.value(), rootPath, columns, alias));
    }

    @Override
    public Class<? extends JsonTableRef> targetType() {
        return JsonTableRef.class;
    }

    private List<JsonTableColumn> parseColumns(Cursor cur, ParseContext ctx) {
        List<JsonTableColumn> columns = new ArrayList<>();
        do {
            columns.add(parseColumn(cur, ctx));
        }
        while (cur.consumeIf(TokenType.COMMA));
        return columns;
    }

    private JsonTableColumn parseColumn(Cursor cur, ParseContext ctx) {
        if (consumeKeyword(cur, "NESTED", TokenType.NESTED)) {
            expectKeyword(cur, "Expected PATH after NESTED", "PATH", TokenType.PATH);
            var path = parsePathLiteral(cur, "Expected nested JSON_TABLE path");
            expectKeyword(cur, "Expected COLUMNS after nested JSON_TABLE path", "COLUMNS", TokenType.COLUMNS);
            cur.expect("Expected '(' after nested JSON_TABLE COLUMNS", TokenType.LPAREN);
            var columns = parseColumns(cur, ctx);
            cur.expect("Expected ')' after nested JSON_TABLE COLUMNS", TokenType.RPAREN);
            return JsonTableNestedPathColumn.of(path, columns);
        }

        var name = toIdentifier(cur.expect("Expected JSON_TABLE column name", TokenType.IDENT));
        if (cur.consumeIf(TokenType.FOR)) {
            cur.expect("Expected ORDINALITY after FOR", TokenType.ORDINALITY);
            return JsonTableOrdinalityColumn.of(name);
        }

        var type = ctx.parse(TypeName.class, cur);
        if (type.isError()) {
            throw new ParserException(type.problems().getFirst().message(), type.problems().getFirst().pos());
        }

        if (cur.consumeIf(TokenType.EXISTS)) {
            expectKeyword(cur, "Expected PATH after EXISTS", "PATH", TokenType.PATH);
            var path = parsePathLiteral(cur, "Expected EXISTS JSON_TABLE path");
            var onError = parseOptionalBehavior(cur, "ERROR", TokenType.ERROR);
            return JsonTableExistsColumn.of(name, type.value(), path, onError);
        }

        expectKeyword(cur, "Expected PATH after JSON_TABLE scalar column type", "PATH", TokenType.PATH);
        var path = parsePathLiteral(cur, "Expected scalar JSON_TABLE path");
        var wrapper = parseWrapper(cur);
        var onEmpty = parseOptionalBehavior(cur, "EMPTY", TokenType.EMPTY);
        var onError = parseOptionalBehavior(cur, "ERROR", TokenType.ERROR);
        return JsonTableScalarColumn.of(name, type.value(), path, wrapper, onEmpty, onError);
    }

    private JsonPathSpec parsePathLiteral(Cursor cur, String message) {
        return JsonPathSpec.of(cur.expect(message, TokenType.STRING).lexeme());
    }

    private JsonTableScalarColumn.Wrapper parseWrapper(Cursor cur) {
        if (cur.consumeIf(TokenType.WITHOUT)) {
            expectKeyword(cur, "Expected WRAPPER after WITHOUT", "WRAPPER", TokenType.WRAPPER);
            return JsonTableScalarColumn.Wrapper.WITHOUT;
        }
        if (cur.consumeIf(TokenType.WITH)) {
            boolean conditional = false;
            if (cur.match(TokenType.IDENT) && "CONDITIONAL".equalsIgnoreCase(cur.peek().lexeme())) {
                conditional = true;
                cur.advance();
            }
            expectKeyword(cur, "Expected WRAPPER after WITH", "WRAPPER", TokenType.WRAPPER);
            return conditional ? JsonTableScalarColumn.Wrapper.CONDITIONAL : JsonTableScalarColumn.Wrapper.WITH;
        }
        return JsonTableScalarColumn.Wrapper.DEFAULT;
    }

    private JsonTableBehavior parseOptionalBehavior(Cursor cur, String conditionLexeme, TokenType conditionType) {
        if (consumeKeyword(cur, "ERROR", TokenType.ERROR)) {
            cur.expect("Expected ON after ERROR", TokenType.ON);
            expectKeyword(cur, "Expected " + conditionLexeme + " after ON", conditionLexeme, conditionType);
            return JsonTableBehavior.of(JsonTableBehavior.Kind.ERROR);
        }
        if (cur.consumeIf(TokenType.NULL)) {
            cur.expect("Expected ON after NULL", TokenType.ON);
            expectKeyword(cur, "Expected " + conditionLexeme + " after ON", conditionLexeme, conditionType);
            return JsonTableBehavior.of(JsonTableBehavior.Kind.NULL);
        }
        if (consumeKeyword(cur, "EMPTY", TokenType.EMPTY)) {
            cur.expect("Expected ON after EMPTY", TokenType.ON);
            expectKeyword(cur, "Expected " + conditionLexeme + " after ON", conditionLexeme, conditionType);
            return JsonTableBehavior.of(JsonTableBehavior.Kind.EMPTY);
        }
        return null;
    }

    private static Token expectKeyword(Cursor cur, String message, String lexeme, TokenType type) {
        return cur.expect(message, token -> isKeyword(token, lexeme, type));
    }

    private static boolean consumeKeyword(Cursor cur, String lexeme, TokenType type) {
        return cur.consumeIf(token -> isKeyword(token, lexeme, type));
    }

    private static boolean isKeyword(Token token, String lexeme, TokenType type) {
        return token.type() == type || token.type() == TokenType.IDENT && token.lexeme().equalsIgnoreCase(lexeme);
    }
}
