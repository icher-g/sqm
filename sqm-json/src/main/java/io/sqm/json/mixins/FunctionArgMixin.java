package io.sqm.json.mixins;

/* -------------------------
 * FunctionExpr + Arg family
 * ------------------------- */

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.internal.FunctionArgExpr;
import io.sqm.core.internal.FuncStarArg;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = FunctionArgExpr.class, name = "arg_expr"),
    @JsonSubTypes.Type(value = FuncStarArg.class, name = "arg_star")
})
public abstract class FunctionArgMixin extends CommonJsonMixin {
}
