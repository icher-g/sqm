package io.sqm.json.mixins;

/* -------------------------
 * FunctionExpr + Arg family
 * ------------------------- */

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.internal.FuncCallArg;
import io.sqm.core.internal.FuncColumnArg;
import io.sqm.core.internal.FuncLiteralArg;
import io.sqm.core.internal.FuncStarArg;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = FuncColumnArg.class, name = "arg_column"),
    @JsonSubTypes.Type(value = FuncLiteralArg.class, name = "arg_literal"),
    @JsonSubTypes.Type(value = FuncCallArg.class, name = "arg_function"),
    @JsonSubTypes.Type(value = FuncStarArg.class, name = "arg_star")
})
public abstract class FunctionArgMixin extends CommonJsonMixin {
}
