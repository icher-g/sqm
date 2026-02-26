package io.sqm.json.mixins;

/* ===========================
 * Top-level: Params family
 * =========================== */

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.AnonymousParamExpr;
import io.sqm.core.NamedParamExpr;
import io.sqm.core.OrdinalParamExpr;

/**
 * Jackson mixin root for parameter-expression polymorphism.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AnonymousParamExpr.Impl.class, name = "anonymous-param"),
    @JsonSubTypes.Type(value = NamedParamExpr.Impl.class, name = "named-param"),
    @JsonSubTypes.Type(value = OrdinalParamExpr.Impl.class, name = "ordinal-param")
})
public abstract class ParamExprMixin extends CommonJsonMixin {

    /**
     * Creates parameter-expression mixin metadata.
     */
    protected ParamExprMixin() {
    }
}
