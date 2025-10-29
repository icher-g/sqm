package io.sqm.json.mixins;

/* =========
 * JOIN tree
 * ========= */

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.internal.CrossJoinImpl;
import io.sqm.core.internal.NaturalJoinImpl;
import io.sqm.core.internal.OnJoinImpl;
import io.sqm.core.internal.UsingJoinImpl;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CrossJoinImpl.class, name = "cross"),
    @JsonSubTypes.Type(value = NaturalJoinImpl.class, name = "natural"),
    @JsonSubTypes.Type(value = OnJoinImpl.class, name = "on"),
    @JsonSubTypes.Type(value = UsingJoinImpl.class, name = "using")
})
public abstract class JoinMixin extends CommonJsonMixin {
}