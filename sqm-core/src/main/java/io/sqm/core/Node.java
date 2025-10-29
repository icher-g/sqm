package io.sqm.core;

import java.io.Serializable;

/**
 * Root of the SQM tree.
 */
public sealed interface Node extends Serializable permits CteDef, Expression, GroupBy, GroupItem, Join, LimitOffset, OrderBy, OrderItem, Query, SelectItem, TableRef, WhenThen {
}
