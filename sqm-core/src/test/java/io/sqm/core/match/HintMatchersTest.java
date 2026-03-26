package io.sqm.core.match;

import io.sqm.core.HintArg;
import io.sqm.core.QualifiedName;
import io.sqm.core.StatementHint;
import io.sqm.core.TableHint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HintMatchersTest {

    @Test
    void hintMatchSelectsStatementAndFallsBackOtherwise() {
        var statementHint = StatementHint.of("MAX_EXECUTION_TIME", 1000);
        var tableHint = TableHint.of("NOLOCK");

        var statementResult = statementHint.matchHint()
            .statement(h -> "statement:" + h.name().value())
            .table(h -> "table:" + h.name().value())
            .otherwise(h -> "other");

        var tableResult = tableHint.matchHint()
            .statement(h -> "statement:" + h.name().value())
            .table(h -> "table:" + h.name().value())
            .otherwise(h -> "other");

        assertEquals("statement:MAX_EXECUTION_TIME", statementResult);
        assertEquals("table:NOLOCK", tableResult);
    }

    @Test
    void hintArgMatchSelectsTypedVariantAndFallsBackOtherwise() {
        var identifier = HintArg.identifier("users");
        var qualifiedName = HintArg.qualifiedName(QualifiedName.of("app", "users"));
        var expression = HintArg.expression(io.sqm.core.Expression.literal(1000));

        var identifierResult = identifier.matchHintArg()
            .identifier(arg -> "identifier:" + arg.value().value())
            .qualifiedName(arg -> "qualified")
            .expression(arg -> "expression")
            .otherwise(arg -> "other");
        var qualifiedResult = qualifiedName.matchHintArg()
            .identifier(arg -> "identifier")
            .qualifiedName(arg -> "qualified:" + String.join(".", arg.value().values()))
            .expression(arg -> "expression")
            .otherwise(arg -> "other");
        var expressionResult = expression.matchHintArg()
            .identifier(arg -> "identifier")
            .qualifiedName(arg -> "qualified")
            .expression(arg -> "expression:" + arg.value().matchExpression().literal(l -> l.value()).orElseThrow(IllegalStateException::new))
            .otherwise(arg -> "other");

        assertEquals("identifier:users", identifierResult);
        assertEquals("qualified:app.users", qualifiedResult);
        assertEquals("expression:1000", expressionResult);
    }
}
