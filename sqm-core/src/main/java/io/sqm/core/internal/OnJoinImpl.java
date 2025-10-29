package io.sqm.core.internal;

import io.sqm.core.JoinKind;
import io.sqm.core.OnJoin;
import io.sqm.core.Predicate;
import io.sqm.core.TableRef;

/**
 * Implements a regular join: INNER/LEFT/RIGHT/FULL.
 *
 * @param right the table to join.
 * @param kind  the join type: INNER/LEFT/RIGHT/FULL
 * @param on    the predicate to use on the join.
 */
public record OnJoinImpl(TableRef right, JoinKind kind, Predicate on) implements OnJoin {
}
