package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class GroupMixIn {
    @JsonIgnore
    abstract boolean isOrdinal();
}
