package io.sqm.core.collect;

import io.sqm.core.AnonymousParamExpr;
import io.sqm.core.NamedParamExpr;
import io.sqm.core.OrdinalParamExpr;
import io.sqm.core.ParamExpr;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ParametersCollectorTest {

    @Test
    void collectsOrdinalParametersAsPositional() {
        ParametersCollector collector = new ParametersCollector();

        // Adjust construction to your real API:
        // e.g. OrdinalParamExpr.of(1), new OrdinalParamExpr(1), etc.
        OrdinalParamExpr p1 = OrdinalParamExpr.of(1);
        OrdinalParamExpr p2 = OrdinalParamExpr.of(2);

        collector.visitOrdinalParamExpr(p1);
        collector.visitOrdinalParamExpr(p2);

        List<ParamExpr> positional = collector.positional();
        List<ParamExpr> named = collector.named();

        assertEquals(2, positional.size(), "Expected two positional parameters");
        assertTrue(positional.contains(p1));
        assertTrue(positional.contains(p2));

        assertTrue(named.isEmpty(), "No named parameters expected");
    }

    @Test
    void collectsAnonymousParametersAsPositional() {
        ParametersCollector collector = new ParametersCollector();

        // Adjust construction to your real API:
        AnonymousParamExpr p1 = AnonymousParamExpr.of();
        AnonymousParamExpr p2 = AnonymousParamExpr.of();

        collector.visitAnonymousParamExpr(p1);
        collector.visitAnonymousParamExpr(p2);

        List<ParamExpr> positional = collector.positional();
        List<ParamExpr> named = collector.named();

        assertEquals(2, positional.size(), "Expected two positional parameters");
        assertTrue(positional.contains(p1));
        assertTrue(positional.contains(p2));

        assertTrue(named.isEmpty(), "No named parameters expected");
    }

    @Test
    void collectsNamedParametersSeparately() {
        ParametersCollector collector = new ParametersCollector();

        // Adjust construction to your real API:
        NamedParamExpr p1 = NamedParamExpr.of("id");
        NamedParamExpr p2 = NamedParamExpr.of("tenantId");

        collector.visitNamedParamExpr(p1);
        collector.visitNamedParamExpr(p2);

        List<ParamExpr> positional = collector.positional();
        List<ParamExpr> named = collector.named();

        assertTrue(positional.isEmpty(), "No positional parameters expected");
        assertEquals(2, named.size(), "Expected two named parameters");
        assertTrue(named.contains(p1));
        assertTrue(named.contains(p2));
    }

    @Test
    void collectsMixedParametersAndPreservesVisitOrder() {
        ParametersCollector collector = new ParametersCollector();

        // Example mixed set in a hypothetical query:
        // SELECT * FROM t WHERE a = $1 AND b = :name AND c = ? AND d = $2 AND e = :tenant
        OrdinalParamExpr pOrdinal1 = OrdinalParamExpr.of(1);     // $1
        NamedParamExpr pNamed1 = NamedParamExpr.of("name");  // :name
        AnonymousParamExpr pAnon = AnonymousParamExpr.of();    // ?
        OrdinalParamExpr pOrdinal2 = OrdinalParamExpr.of(2);     // $2
        NamedParamExpr pNamed2 = NamedParamExpr.of("tenant");// :tenant

        // Simulate traversal order by calling visitor methods in that order
        collector.visitOrdinalParamExpr(pOrdinal1);
        collector.visitNamedParamExpr(pNamed1);
        collector.visitAnonymousParamExpr(pAnon);
        collector.visitOrdinalParamExpr(pOrdinal2);
        collector.visitNamedParamExpr(pNamed2);

        List<ParamExpr> positional = collector.positional();
        List<ParamExpr> named = collector.named();

        // Positional should contain ordinal + anonymous in the exact visit order
        assertEquals(3, positional.size(), "Expected three positional parameters");
        assertSame(pOrdinal1, positional.get(0));
        assertSame(pAnon, positional.get(1));
        assertSame(pOrdinal2, positional.get(2));

        // Named should contain only named, in visit order
        assertEquals(2, named.size(), "Expected two named parameters");
        assertSame(pNamed1, named.get(0));
        assertSame(pNamed2, named.get(1));
    }

    @Test
    void noParametersCollectedWhenVisitorIsUnused() {
        ParametersCollector collector = new ParametersCollector();

        assertTrue(collector.positional().isEmpty());
        assertTrue(collector.named().isEmpty());
    }
}
