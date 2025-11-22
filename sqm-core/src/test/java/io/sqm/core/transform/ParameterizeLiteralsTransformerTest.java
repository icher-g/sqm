package io.sqm.core.transform;

import io.sqm.core.LiteralExpr;
import io.sqm.core.NamedParamExpr;
import io.sqm.core.Node;
import io.sqm.core.ParamExpr;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ParameterizeLiteralsTransformer}.
 */
class ParameterizeLiteralsTransformerTest {

    private final ParameterizeLiteralsTransformer transformer = new ParameterizeLiteralsTransformer((i) -> NamedParamExpr.of("_p" + i));

    @Test
    void singleLiteralIsReplacedByNamedParameter() {
        // given
        LiteralExpr literal = LiteralExpr.of("ACTIVE");

        // when
        Node transformed = transformer.transform(literal);

        // then
        assertInstanceOf(NamedParamExpr.class, transformed, "Literal should be replaced by NamedParamExpr");

        NamedParamExpr param = (NamedParamExpr) transformed;
        assertNotNull(param.name(), "Parameter name should not be null");
        assertTrue(param.name().startsWith("_p"),
            "Generated parameter name should start with _p");

        Map<ParamExpr, Object> values = transformer.valuesByParam();
        assertEquals(1, values.size(), "Exactly one literal should have been recorded");
        assertEquals("ACTIVE", values.get(param), "Parameter should map to original literal value");
    }

    @Test
    void multipleLiteralsProduceUniqueParameters() {
        // given
        LiteralExpr lit1 = LiteralExpr.of("ACTIVE");
        LiteralExpr lit2 = LiteralExpr.of(42);
        LiteralExpr lit3 = LiteralExpr.of(true);

        // when
        NamedParamExpr p1 = (NamedParamExpr) transformer.transform(lit1);
        NamedParamExpr p2 = (NamedParamExpr) transformer.transform(lit2);
        NamedParamExpr p3 = (NamedParamExpr) transformer.transform(lit3);

        // then
        assertNotEquals(p1.name(), p2.name(), "Each literal must get a unique parameter name");
        assertNotEquals(p1.name(), p3.name(), "Each literal must get a unique parameter name");
        assertNotEquals(p2.name(), p3.name(), "Each literal must get a unique parameter name");

        Map<ParamExpr, Object> values = transformer.valuesByParam();
        assertEquals(3, values.size(), "All three literals should be recorded");

        assertEquals("ACTIVE", values.get(p1));
        assertEquals(42, values.get(p2));
        assertEquals(true, values.get(p3));
    }

    @Test
    void existingParametersAreNotChangedOrRecorded() {
        // given
        NamedParamExpr existingParam = NamedParamExpr.of("id");

        // when
        Node transformed = transformer.transform(existingParam);

        // then
        assertSame(existingParam, transformed,
            "Existing ParamExpr instances should be left unchanged");
        assertTrue(transformer.valuesByParam().isEmpty(),
            "No literal values should be recorded when only parameters are present");
    }

    @Test
    void valuesByParamMapIsImmutableView() {
        // given
        LiteralExpr literal = LiteralExpr.of("ACTIVE");
        transformer.transform(literal);

        Map<ParamExpr, Object> values = transformer.valuesByParam();

        // when / then
        assertThrows(UnsupportedOperationException.class, () -> values.clear(), "Returned map should be immutable");
    }

    @Test
    void transformMethodUsesVisitorAndReturnsSameStaticType() {
        // This test is mostly about the generic <N extends Node> transform(N root) contract.

        // given
        LiteralExpr literal = LiteralExpr.of(123);

        // when
        Node result = transformer.transform(literal);

        // then
        assertInstanceOf(NamedParamExpr.class, result, "LiteralExpr should be transformed into NamedParamExpr via transform(root)");
    }
}