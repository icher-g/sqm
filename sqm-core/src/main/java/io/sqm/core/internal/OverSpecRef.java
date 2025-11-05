package io.sqm.core.internal;

import io.sqm.core.OverSpec;

/**
 * OVER w — reference a named window from the SELECT's WINDOW clause
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     AVG(salary) OVER w
 *     }
 * </pre>
 *
 * @param windowName a window name.
 */
public record OverSpecRef(String windowName) implements OverSpec.Ref {
}
