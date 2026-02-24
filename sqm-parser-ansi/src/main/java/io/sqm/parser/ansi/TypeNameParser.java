package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.ParserException;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

public class TypeNameParser implements Parser<TypeName> {

    private static final Set<String> keywords = Set.of("double", "character", "national");
    private static final Set<String> validTimeTypes = Set.of("time", "timestamp");

    private static TypeKeyword parseKeyword(String firstPart, Cursor cur) {
        if ("double".equalsIgnoreCase(firstPart)) {
            var t = cur.expect("Expected identifier", TokenType.IDENT);
            if (t.lexeme().equalsIgnoreCase("precision")) {
                return TypeKeyword.DOUBLE_PRECISION;
            }
            throw new ParserException("Expected precision after double", cur.fullPos());
        }
        if ("character".equalsIgnoreCase(firstPart)) {
            var t = cur.expect("Expected identifier", TokenType.IDENT);
            if (t.lexeme().equalsIgnoreCase("varying")) {
                return TypeKeyword.CHARACTER_VARYING;
            }
            throw new ParserException("Expected varying after character", cur.fullPos());
        }

        var t = cur.expect("Expected identifier", TokenType.IDENT);
        if (!t.lexeme().equalsIgnoreCase("character")) {
            throw new ParserException("Expected character after national", cur.fullPos());
        }

        if (cur.peek().lexeme().equalsIgnoreCase("varying")) {
            cur.advance();
            return TypeKeyword.NATIONAL_CHARACTER_VARYING;
        }
        return TypeKeyword.NATIONAL_CHARACTER;
    }

    private static List<Expression> parseModifiers(Cursor cur, ParseContext ctx) {
        List<Expression> expressions = new ArrayList<>();
        do {
            var result = ctx.parse(Expression.class, cur);
            if (!result.ok()) {
                var problem = result.problems().getFirst();
                throw new ParserException(problem.message(), problem.pos());
            }
            expressions.add(result.value());
        }
        while (cur.consumeIf(TokenType.COMMA));
        return expressions;
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends TypeName> parse(Cursor cur, ParseContext ctx) {
        var name = cur.expect("Expected identifier", TokenType.IDENT);

        TypeKeyword keyword = null;
        QualifiedName parts = null;
        List<Expression> modifiers = null;
        TimeZoneSpec timeZoneSpec = TimeZoneSpec.NONE;
        int arrayDims = 0;

        if (!name.quotedIdentifier() && keywords.contains(name.lexeme().toLowerCase(Locale.ROOT))) {
            keyword = parseKeyword(name.lexeme(), cur);
        }

        if (cur.match(TokenType.DOT)) {
            parts = parseQualifiedName(toIdentifier(name), cur);
        }

        // this is a single word.
        if (parts == null && keyword == null) {
            parts = QualifiedName.of(toIdentifier(name));
        }

        if (cur.consumeIf(TokenType.LPAREN)) {
            modifiers = parseModifiers(cur, ctx);
            cur.expect("Expected )", TokenType.RPAREN);
        }

        if (cur.matchAny(TokenType.WITH, TokenType.WITHOUT)) {
            var lexeme = cur.peek().lexeme().toLowerCase(Locale.ROOT);
            if (!validTimeTypes.contains(name.lexeme().toLowerCase(Locale.ROOT))) {
                return error("Only time and timestamp are supported with time zone", cur.fullPos());
            }
            cur.advance(); // skip with or without.
            var t = cur.expect("Expected identifier 'time'", TokenType.IDENT);
            if (!t.lexeme().equalsIgnoreCase("time")) {
                return error("Expected 'time' but found '" + t.lexeme() + "'", cur.fullPos());
            }
            t = cur.expect("Expected identifier 'zone'", TokenType.IDENT);
            if (!t.lexeme().equalsIgnoreCase("zone")) {
                return error("Expected 'zone' but found '" + t.lexeme() + "'", cur.fullPos());
            }
            timeZoneSpec = lexeme.equals("with") ? TimeZoneSpec.WITH_TIME_ZONE : TimeZoneSpec.WITHOUT_TIME_ZONE;
        }

        while (cur.consumeIf(TokenType.LBRACKET)) {
            cur.expect("Expected ] to close the array type", TokenType.RBRACKET);
            arrayDims++;
        }

        return ok(TypeName.of(parts, keyword, modifiers, arrayDims, timeZoneSpec));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends TypeName> targetType() {
        return TypeName.class;
    }
}

