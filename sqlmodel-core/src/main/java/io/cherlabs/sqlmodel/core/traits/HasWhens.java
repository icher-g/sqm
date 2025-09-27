package io.cherlabs.sqlmodel.core.traits;

import io.cherlabs.sqlmodel.core.WhenThen;

import java.util.List;

public interface HasWhens {
    List<WhenThen> whens();
}
