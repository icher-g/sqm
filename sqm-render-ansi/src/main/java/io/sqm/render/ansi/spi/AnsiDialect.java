package io.sqm.render.ansi.spi;

import io.sqm.core.Node;
import io.sqm.core.OrdinalParamExpr;
import io.sqm.core.collect.ParametersCollector;
import io.sqm.core.transform.ParameterizeLiteralsTransformer;
import io.sqm.render.ansi.DefaultValueFormatter;
import io.sqm.render.ansi.Renderers;
import io.sqm.render.spi.*;

public class AnsiDialect implements SqlDialect {

    private final IdentifierQuoter quoter = new AnsiIdentifierQuoter();
    private final ValueFormatter formatter = new DefaultValueFormatter(this);
    private final Operators operators = new AnsiOperators();
    private final Booleans booleans = new AnsiBooleans();
    private final NullSorting nullSorting = new AnsiNullSorting();
    private final PaginationStyle paginationStyle = new AnsiPaginationStyle();
    private final RenderersRepository repo = Renderers.ansi();

    @Override
    public PreparedNode beforeRender(Node root, RenderOptions options) {
        if (options.parameterizationMode() == ParameterizationMode.Bind) {
            var collector = new ParametersCollector();
            root.accept(collector);

            if (collector.positional().isEmpty() && collector.named().isEmpty()) {
                // convert all literals to params.
                var literalsTransformer = new ParameterizeLiteralsTransformer((i) -> OrdinalParamExpr.of(i));
                root = root.accept(literalsTransformer);

                return PreparedNode.of(root, literalsTransformer.values());
            }
            throw new IllegalStateException("BIND parameterization mode is not supported for query that already has parameters.");
        }
        return SqlDialect.super.beforeRender(root, options);
    }

    @Override
    public String name() {
        return "ansi";
    }

    @Override
    public IdentifierQuoter quoter() {
        return quoter;
    }

    @Override
    public ValueFormatter formatter() {
        return formatter;
    }

    @Override
    public Operators operators() {
        return operators;
    }

    @Override
    public Booleans booleans() {
        return booleans;
    }

    @Override
    public NullSorting nullSorting() {
        return nullSorting;
    }

    @Override
    public PaginationStyle paginationStyle() {
        return paginationStyle;
    }

    @Override
    public RenderersRepository renderers() {
        return repo;
    }
}
