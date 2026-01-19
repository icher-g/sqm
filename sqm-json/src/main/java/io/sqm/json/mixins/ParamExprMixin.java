package io.sqm.json.mixins;

/* ===========================
 * Top-level: Params family
 * =========================== */

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.internal.AnonymousParamExprImpl;
import io.sqm.core.internal.NamedParamExprImpl;
import io.sqm.core.internal.OrdinalParamExprImpl;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AnonymousParamExprImpl.class, name = "anonymous-param"),
    @JsonSubTypes.Type(value = NamedParamExprImpl.class, name = "named-param"),
    @JsonSubTypes.Type(value = OrdinalParamExprImpl.class, name = "ordinal-param")
})
public abstract class ParamExprMixin extends CommonJsonMixin {
}
