package io.cherlabs.sqlmodel.core.traits;

import io.cherlabs.sqlmodel.core.Join;

public interface HasJoinType {
    Join.JoinType joinType();
}
