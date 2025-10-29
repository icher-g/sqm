package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class GroupItemMixin extends CommonJsonMixin {
    // Prevent helpers from leaking into JSON (adjust if your method name differs)
    @JsonIgnore
    boolean isOrdinal() { return false; }
}
