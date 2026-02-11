package io.sqm.validate.schema.internal;

import io.sqm.core.*;

import java.util.Arrays;

/**
 * Provides stable human-readable node kind labels for diagnostics.
 */
final class NodeKinds {
    private NodeKinds() {
    }

    /**
     * Returns stable node kind label for diagnostics.
     *
     * @param node SQM node.
     * @return node kind label.
     */
    static String of(Node node) {
        if (node == null) {
            return null;
        }
        var impl = node.getClass();
        var nodeInterface = Arrays.stream(impl.getInterfaces())
            .filter(Node.class::isAssignableFrom)
            .findFirst();
        return nodeInterface.map(Class::getSimpleName).orElseGet(() -> fallback(node));
    }

    /**
     * Returns fallback node kind when no explicit mapping exists.
     *
     * @param node SQM node.
     * @return fallback label.
     */
    private static String fallback(Node node) {
        var simpleName = node.getClass().getSimpleName();
        if (simpleName.endsWith("Impl") && simpleName.length() > 4) {
            return simpleName.substring(0, simpleName.length() - 4);
        }
        return simpleName;
    }
}
