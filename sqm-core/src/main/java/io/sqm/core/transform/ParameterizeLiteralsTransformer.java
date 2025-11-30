package io.sqm.core.transform;

import io.sqm.core.*;

import java.util.*;
import java.util.function.Function;

/**
 * A {@link RecursiveNodeTransformer} that replaces every {@link LiteralExpr}
 * in the tree with a newly created {@link ParamExpr}.
 * <p>
 * This transformer is the first step of the parameterization pipeline used in
 * {@code ParameterizationMode.Bind}. It systematically scans the SQL AST and:
 *
 * <ol>
 *   <li>Creates a new parameter expression for each literal using the supplied
 *       {@code paramCreator} function.</li>
 *   <li>Stores a mapping of {@code ParamExpr -> literal value} in the order in
 *       which literals appear in the tree.</li>
 *   <li>Returns a transformed tree where all {@link LiteralExpr} nodes have
 *       been replaced with the corresponding {@link ParamExpr} instances.</li>
 * </ol>
 *
 * <p>Parameter Ordering</p>
 * The {@code counter} starts at {@code 1} and increments once for each literal
 * encountered. The ordering is therefore:
 * <ul>
 *     <li>stable,</li>
 *     <li>deterministic,</li>
 *     <li>left-to-right depth-first traversal order (as defined by the visitor).</li>
 * </ul>
 *
 * <p>
 * The {@code paramCreator} function receives the 1-based ordinal index of the
 * literal being replaced and can generate any {@link ParamExpr} subtype,
 * including:
 * <ul>
 *     <li>{@link NamedParamExpr} (e.g., {@code :p1}, {@code @p1})</li>
 *     <li>anonymous parameters (e.g., {@code ?})</li>
 *     <li>ordinal positional parameters (e.g., {@code $1}, {@code $2})</li>
 * </ul>
 * This allows dialects to control the specific flavor of parameter expressions
 * used during literal parameterization.
 *
 * <p>Collected Values</p>
 * Two value collections are maintained:
 *
 * <ul>
 *   <li>{@link #valuesByParam()} — a {@code Map<ParamExpr, Object>} describing
 *       each generated parameter and the literal value it replaced.</li>
 *
 *   <li>{@link #values()} — a read-only {@code List<Object>} of literal values
 *       in their encounter order, useful when the caller only needs a positional
 *       list.</li>
 * </ul>
 * <p>
 * Both collections preserve creation order by using {@link LinkedHashMap} and
 * {@link ArrayList}.
 *
 * <p>Example</p>
 * <p>
 * Suppose the tree contains:
 * <pre>
 *   WHERE a = 10 AND b = 'x'
 * </pre>
 * <p>
 * With {@code paramCreator(i -> NamedParamExpr.of("p" + i))}, the result is:
 *
 * <pre>
 *   WHERE a = :p1 AND b = :p2
 * </pre>
 * <p>
 * and:
 * <pre>
 *   valuesByParam = { :p1 -> 10, :p2 -> "x" }
 *   values = [10, "x"]
 * </pre>
 *
 * <p>
 * This transformer does not perform any dialect-specific validation or rendering;
 * it only rewrites literals to parameters. Further processing (e.g., rewriting
 * unsupported parameter styles) is handled by later transformers such as
 * {@code AnonymousParamsTransformer}.
 */
public final class ParameterizeLiteralsTransformer extends RecursiveNodeTransformer {

    private final Function<Integer, ParamExpr> paramCreator;
    private final Map<ParamExpr, Object> params = new LinkedHashMap<>();
    private final List<Object> values = new ArrayList<>();
    private int counter = 1;

    /**
     * Creates an instance of {@link ParameterizeLiteralsTransformer}.
     *
     * @param paramCreator a param creation function.
     */
    public ParameterizeLiteralsTransformer(Function<Integer, ParamExpr> paramCreator) {
        this.paramCreator = paramCreator;
    }

    /**
     * Returns an unmodifiable mapping of generated parameters to the literal
     * values they replaced, in encounter order.
     */
    public Map<ParamExpr, Object> valuesByParam() {
        return Collections.unmodifiableMap(params);
    }

    /**
     * Returns an unmodifiable list of literal values in encounter order.
     * This is effectively the positional parameter list.
     */
    public List<Object> values() {
        return Collections.unmodifiableList(values);
    }

    /**
     * Visits a {@link LiteralExpr} and replaces it with a {@link ParamExpr}.
     *
     * @param l the literal expression being visited
     * @return a new {@link ParamExpr} representing a bind parameter.
     */
    @Override
    public Node visitLiteralExpr(LiteralExpr l) {
        var param = paramCreator.apply(counter++);
        params.put(param, l.value());
        values.add(l.value());
        return param;
    }
}
