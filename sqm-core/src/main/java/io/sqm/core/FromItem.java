package io.sqm.core;

public sealed interface FromItem extends Node permits TableRef, Join {
}
