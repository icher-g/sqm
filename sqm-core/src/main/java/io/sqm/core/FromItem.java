package io.sqm.core;

/**
 * An interface to represent a FROM statement.
 */
public sealed interface FromItem extends Node permits TableRef, Join {
}
