package io.sqm.render.ansi;

import io.sqm.core.NamedParamExpr;
import io.sqm.core.Node;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.ParameterizationMode;
import io.sqm.render.spi.PreparedNode;
import io.sqm.render.spi.RenderOptions;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link io.sqm.render.ansi.spi.AnsiDialect#beforeRender(Node, RenderOptions)} in Bind mode.
 */
class BeforeRenderBindModeTest {

    // -------------------------------------------------------------------------
    // All values come from literals (via ParameterizeLiteralsTransformer)
    // -------------------------------------------------------------------------

    @Test
    void bindMode_collectsValuesFromLiteralsInParameterOrder() {
        // given: a query whose only parameters come from literals
        Node root = queryWithTwoLiteralPredicates(); // e.g. WHERE a = 10 AND b = 'x'

        RenderOptions options = RenderOptions.of(
            ParameterizationMode.Bind
        );

        var dialect = new AnsiDialect();

        // when
        PreparedNode prepared = dialect.beforeRender(root, options);

        // then
        var params = prepared.params();

        // Expect two positional values coming from literals, in the order
        // defined by AnonymousParamsTransformer.paramsByIndex().
        assertEquals(2, params.size(), "Expected two positional parameters");

        // Adjust these expected values to match your literal values
        assertEquals(10, params.get(0));
        assertEquals("x", params.get(1));
    }

    // -------------------------------------------------------------------------
    // Missing value for (non-literal, non-named) parameter → IllegalStateException
    // -------------------------------------------------------------------------

    @Test
    void bindMode_throwsWhenNoValueProvidedForParameter() {
        // given: a query with one anonymous / positional parameter that is NOT produced
        // by ParameterizeLiteralsTransformer and NOT a NamedParamExpr.
        Node root = queryWithSingleAnonymousParam();
        RenderOptions options = RenderOptions.of(ParameterizationMode.Bind);

        var dialect = new AnsiDialect();

        // when / then
        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> dialect.beforeRender(root, options)
        );

        assertTrue(
            ex.getMessage().contains("BIND parameterization mode is not supported"),
            "Exception message should mention missing parameter"
        );
    }

    // =====================================================================
    // Helper methods - implement using your DSL/parser
    // =====================================================================

    /**
     * @return a query where all parameters come from literal expressions,
     * e.g. {@code SELECT * FROM t WHERE a = 10 AND b = 'x'}.
     *
     * <p>ParameterizeLiteralsTransformer should turn both literals into params,
     * and AnonymousParamsTransformer should index them as param #1 and #2.
     */
    private Node queryWithTwoLiteralPredicates() {
        return select(star())
            .from(tbl("t"))
            .where(
                col("a").eq(lit(10))
                    .and(
                        col("b").eq(lit("x"))
                    )
            )
            .build();
    }

    /**
     * @return a query with a single anonymous / positional parameter (not from a literal),
     * so that:
     *         <ul>
     *             <li>paramsByIndex().size() == 1</li>
     *             <li>paramsByIndex().get(1) is NOT in valuesByParam()</li>
     *             <li>paramsByIndex().get(1) is NOT a {@link NamedParamExpr}</li>
     *         </ul>
     *
     * <p>This setup should trigger the last {@code else} branch and, with no positional
     * values provided, cause {@link IllegalStateException} to be thrown.
     */
    private Node queryWithSingleAnonymousParam() {
        // WHERE a = ?
        return col("a").eq(param());
    }
}

