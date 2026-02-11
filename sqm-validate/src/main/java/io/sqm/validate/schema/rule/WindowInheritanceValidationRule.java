package io.sqm.validate.schema.rule;

import io.sqm.core.SelectQuery;
import io.sqm.core.WindowDef;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;

import java.util.*;

/**
 * Validates window inheritance references and detects inheritance cycles.
 */
final class WindowInheritanceValidationRule implements SchemaValidationRule<SelectQuery> {
    /**
     * Returns supported node type.
     *
     * @return select query type.
     */
    @Override
    public Class<SelectQuery> nodeType() {
        return SelectQuery.class;
    }

    /**
     * Validates base-window references and inheritance cycles inside WINDOW definitions.
     *
     * @param node select query node.
     * @param context schema validation context.
     */
    @Override
    public void validate(SelectQuery node, SchemaValidationContext context) {
        var windows = indexWindows(node.windows());
        var inheritance = collectInheritance(node.windows(), windows, context);
        detectCycles(windows, inheritance, context);
    }

    /**
     * Indexes windows by normalized name, preserving first declaration.
     *
     * @param windows window definitions.
     * @return windows by normalized name.
     */
    private static Map<String, WindowDef> indexWindows(List<WindowDef> windows) {
        var byName = new LinkedHashMap<String, WindowDef>(windows.size());
        for (var window : windows) {
            if (window.name() == null) {
                continue;
            }
            byName.putIfAbsent(normalize(window.name()), window);
        }
        return byName;
    }

    /**
     * Collects valid inheritance edges and reports missing base windows.
     *
     * @param windows window definitions.
     * @param indexedWindows windows indexed by normalized name.
     * @param context schema validation context.
     * @return inheritance map child->base.
     */
    private static Map<String, String> collectInheritance(
        List<WindowDef> windows,
        Map<String, WindowDef> indexedWindows,
        SchemaValidationContext context
    ) {
        var edges = new LinkedHashMap<String, String>(windows.size());
        for (var window : windows) {
            if (window.name() == null || window.spec() == null || window.spec().baseWindow() == null) {
                continue;
            }

            var from = normalize(window.name());
            var baseRaw = window.spec().baseWindow();
            var to = normalize(baseRaw);
            if (!indexedWindows.containsKey(to)) {
                context.addProblem(
                    ValidationProblem.Code.WINDOW_NOT_FOUND,
                    "Base window not found: " + baseRaw,
                    window,
                    "window.inheritance"
                );
                continue;
            }
            edges.put(from, to);
        }
        return edges;
    }

    /**
     * Detects and reports cycles in window inheritance graph.
     *
     * @param windows windows indexed by normalized name.
     * @param inheritance inheritance edges child->base.
     * @param context schema validation context.
     */
    private static void detectCycles(
        Map<String, WindowDef> windows,
        Map<String, String> inheritance,
        SchemaValidationContext context
    ) {
        var state = new HashMap<String, Integer>(windows.size());
        var stack = new ArrayList<String>(windows.size());
        var position = new HashMap<String, Integer>(windows.size());
        var reported = new HashSet<String>();
        for (var name : windows.keySet()) {
            if (state.getOrDefault(name, 0) == 0) {
                dfs(name, windows, inheritance, state, stack, position, reported, context);
            }
        }
    }

    /**
     * Performs DFS traversal and reports back-edge cycles.
     *
     * @param node current window name.
     * @param windows indexed windows.
     * @param inheritance inheritance edges child->base.
     * @param state DFS color map.
     * @param stack DFS stack.
     * @param position current stack positions.
     * @param reported already reported cycle members.
     * @param context schema validation context.
     */
    private static void dfs(
        String node,
        Map<String, WindowDef> windows,
        Map<String, String> inheritance,
        Map<String, Integer> state,
        List<String> stack,
        Map<String, Integer> position,
        Set<String> reported,
        SchemaValidationContext context
    ) {
        state.put(node, 1);
        position.put(node, stack.size());
        stack.add(node);

        var next = inheritance.get(node);
        if (next != null) {
            var nextState = state.getOrDefault(next, 0);
            if (nextState == 0) {
                dfs(next, windows, inheritance, state, stack, position, reported, context);
            } else if (nextState == 1) {
                reportCycle(next, windows, stack, position, reported, context);
            }
        }

        stack.removeLast();
        position.remove(node);
        state.put(node, 2);
    }

    /**
     * Reports all windows participating in detected cycle.
     *
     * @param cycleStart cycle entry point in current DFS stack.
     * @param windows indexed windows.
     * @param stack DFS stack.
     * @param position current stack positions.
     * @param reported already reported cycle members.
     * @param context schema validation context.
     */
    private static void reportCycle(
        String cycleStart,
        Map<String, WindowDef> windows,
        List<String> stack,
        Map<String, Integer> position,
        Set<String> reported,
        SchemaValidationContext context
    ) {
        var start = position.get(cycleStart);
        if (start == null) {
            return;
        }
        for (int i = start; i < stack.size(); i++) {
            var name = stack.get(i);
            if (!reported.add(name)) {
                continue;
            }
            var window = windows.get(name);
            if (window == null) {
                continue;
            }
            context.addProblem(
                ValidationProblem.Code.WINDOW_INHERITANCE_CYCLE,
                "Window inheritance cycle detected for window: " + window.name(),
                window,
                "window.inheritance"
            );
        }
    }

    /**
     * Normalizes identifier for case-insensitive comparison.
     *
     * @param identifier identifier value.
     * @return normalized identifier.
     */
    private static String normalize(String identifier) {
        return identifier.toLowerCase(Locale.ROOT);
    }
}
