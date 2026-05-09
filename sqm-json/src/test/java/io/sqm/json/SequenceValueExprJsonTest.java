package io.sqm.json;

import io.sqm.core.Expression;
import io.sqm.core.SequenceValueExpr;
import io.sqm.core.SequenceValueKind;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.currentValue;
import static io.sqm.dsl.Dsl.qualify;
import static org.junit.jupiter.api.Assertions.*;

class SequenceValueExprJsonTest {
    @Test
    void roundTripsSequenceValueExpression() throws Exception {
        var mapper = SqmJsonMixins.createPretty();
        var expr = currentValue(qualify("app", "users_seq"));

        var json = mapper.writeValueAsString(expr);
        var back = mapper.readValue(json, Expression.class);

        assertTrue(json.contains("\"kind\" : \"sequence-value\""));
        var sequence = assertInstanceOf(SequenceValueExpr.class, back);
        assertEquals(SequenceValueKind.CURRENT_VALUE, sequence.kind());
        assertEquals(java.util.List.of("app", "users_seq"), sequence.sequence().values());
    }
}
