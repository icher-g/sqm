package io.sqm.json.mixins;

/* ===========================
 * Top-level: Arithmetic Expression family
 * =========================== */

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AddArithmeticExpr.Impl.class, name = "add"),
    @JsonSubTypes.Type(value = SubArithmeticExpr.Impl.class, name = "sub"),
    @JsonSubTypes.Type(value = DivArithmeticExpr.Impl.class, name = "div"),
    @JsonSubTypes.Type(value = ModArithmeticExpr.Impl.class, name = "mod"),
    @JsonSubTypes.Type(value = MulArithmeticExpr.Impl.class, name = "mul"),
    @JsonSubTypes.Type(value = NegativeArithmeticExpr.Impl.class, name = "neg"),
    @JsonSubTypes.Type(value = PowerArithmeticExpr.Impl.class, name = "pow")
})
public abstract class ArithmeticExprMixin extends CommonJsonMixin {
}
