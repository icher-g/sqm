package io.sqm.json.mixins;

/* -------------------------
 * FunctionExpr + Arg family
 * ------------------------- */

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.FunctionExpr;

/**
 * Jackson mixin root for function-argument polymorphism.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = FunctionExpr.Arg.ExprArg.Impl.class, name = "arg_expr"),
    @JsonSubTypes.Type(value = FunctionExpr.Arg.StarArg.Impl.class, name = "arg_star")
})
public abstract class FunctionArgMixin extends CommonJsonMixin {

    /**
     * Creates function-argument mixin metadata.
     */
    protected FunctionArgMixin() {
    }
}
