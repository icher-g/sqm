package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;

/**
 * Hint attached to a table reference.
 */
public non-sealed interface TableHint extends Hint {

    /**
     * Creates a generic table hint from a name and ordered arguments.
     *
     * @param name hint name
     * @param args ordered hint arguments
     * @return table hint
     */
    static TableHint of(Identifier name, List<HintArg> args) {
        return new Impl(name, args);
    }

    /**
     * Creates a generic table hint from a name and ordered arguments.
     *
     * @param name hint name
     * @param args ordered hint arguments
     * @return table hint
     */
    static TableHint of(String name, HintArg... args) {
        return of(Identifier.of(name), List.of(args));
    }

    /**
     * Creates a generic table hint from a name and convenience arguments.
     *
     * @param name hint name
     * @param args ordered convenience hint arguments
     * @return table hint
     */
    static TableHint of(String name, Object... args) {
        return of(Identifier.of(name), HintArg.fromAll(args));
    }

    /**
     * Creates a generic table hint from a quote-aware name and convenience arguments.
     *
     * @param name hint name
     * @param args ordered convenience hint arguments
     * @return table hint
     */
    static TableHint of(Identifier name, Object... args) {
        return of(name, HintArg.fromAll(args));
    }

    /**
     * Accepts a {@link NodeVisitor} that performs an operation on this node.
     * <p>
     * Each concrete node class calls back into the visitor with a type-specific
     * {@code visitXxx(...)} method, allowing the visitor to handle each node
     * type appropriately.
     * </p>
     *
     * @param v   the visitor instance to accept (must not be {@code null})
     * @param <R> the result type produced by the visitor
     * @return the result of the visitor's operation on this node,
     * or {@code null} if the visitor's return type is {@link Void}
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitTableHint(this);
    }

    /**
     * Generic table hint with a normalized name and ordered typed arguments.
     *
     * @param name hint name
     * @param args ordered hint arguments
     */
    record Impl(Identifier name, List<HintArg> args) implements TableHint {

        /**
         * Creates a generic table hint.
         *
         * @param name hint name
         * @param args ordered hint arguments
         */
        public Impl {
            Objects.requireNonNull(name, "name");
            args = args == null ? List.of() : List.copyOf(args);
        }
    }
}