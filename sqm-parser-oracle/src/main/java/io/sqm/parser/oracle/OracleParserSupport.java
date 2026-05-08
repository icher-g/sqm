package io.sqm.parser.oracle;

import io.sqm.core.NamedParamExpr;
import io.sqm.core.OrdinalParamExpr;
import io.sqm.core.ParamExpr;
import io.sqm.core.ResultClause;
import io.sqm.core.ResultItem;
import io.sqm.core.VariableResultTarget;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

final class OracleParserSupport {
    private OracleParserSupport() {
    }

    static ParseResult<ResultClause> parseReturningInto(Parser<?> parser, Cursor cur, ParseContext ctx, String feature) {
        if (!cur.consumeIf(TokenType.RETURNING)) {
            return ok(null);
        }
        if (!ctx.capabilities().supports(SqlFeature.DML_RESULT_CLAUSE)
            || !ctx.capabilities().supports(SqlFeature.DML_RESULT_VARIABLE_TARGET)) {
            return error(feature + " is not supported by this dialect", cur.fullPos());
        }
        var returningResult = parser.parseItems(ResultItem.class, cur, ctx);
        if (returningResult.isError()) {
            return error(returningResult);
        }
        var variables = parseIntoVariables(cur);
        if (variables.isError()) {
            return error(variables);
        }
        return ok(ResultClause.of(returningResult.value(), VariableResultTarget.of(variables.value())));
    }

    static ParseResult<List<ParamExpr>> parseIntoVariables(Cursor cur) {
        cur.expect("Expected INTO after RETURNING projection", TokenType.INTO);

        var variables = new ArrayList<ParamExpr>();
        do {
            var variable = parseVariable(cur);
            if (variable.isError()) {
                return error(variable);
            }
            variables.add(variable.value());

        } while (cur.consumeIf(TokenType.COMMA));
        return ok(variables);
    }

    private static ParseResult<ParamExpr> parseVariable(Cursor cur) {
        if (!cur.match(TokenType.COLON)) {
            return error("Expected Oracle bind variable after INTO", cur.fullPos());
        }

        var colon = cur.expect("Expected :", TokenType.COLON);
        if (cur.match(TokenType.NUMBER)) {
            var number = cur.expect("Expected bind number after :", TokenType.NUMBER);
            if (!colon.isImmediatelyFollowedBy(number)) {
                return error("The number must be aligned to a :", cur.fullPos());
            }
            try {
                return ok(OrdinalParamExpr.of(Integer.parseInt(number.lexeme())));
            }
            catch (NumberFormatException ex) {
                return error("Expected integer bind number after :", number.pos());
            }
        }

        if (cur.match(TokenType.IDENT)) {
            var identifier = cur.expect("Expected bind name after :", TokenType.IDENT);
            if (!colon.isImmediatelyFollowedBy(identifier)) {
                return error("The identifier must be aligned to a :", cur.fullPos());
            }
            return ok(NamedParamExpr.of(identifier.lexeme()));
        }

        return error("Expected bind name or number after :", cur.fullPos());
    }
}
