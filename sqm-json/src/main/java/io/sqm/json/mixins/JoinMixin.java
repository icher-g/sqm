package io.sqm.json.mixins;

/* =========
 * JOIN tree
 * ========= */

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.CrossJoin;
import io.sqm.core.NaturalJoin;
import io.sqm.core.OnJoin;
import io.sqm.core.UsingJoin;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CrossJoin.Impl.class, name = "cross"),
    @JsonSubTypes.Type(value = NaturalJoin.Impl.class, name = "natural"),
    @JsonSubTypes.Type(value = OnJoin.Impl.class, name = "on"),
    @JsonSubTypes.Type(value = UsingJoin.Impl.class, name = "using")
})
public abstract class JoinMixin extends CommonJsonMixin {
}