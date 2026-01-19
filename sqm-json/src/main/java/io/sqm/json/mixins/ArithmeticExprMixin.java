package io.sqm.json.mixins;

/* ===========================
 * Top-level: Arithmetic Expression family
 * =========================== */

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.internal.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AddArithmeticExprImpl.class, name = "add"),
    @JsonSubTypes.Type(value = SubArithmeticExprImpl.class, name = "sub"),
    @JsonSubTypes.Type(value = DivArithmeticExprImpl.class, name = "div"),
    @JsonSubTypes.Type(value = ModArithmeticExprImpl.class, name = "mod"),
    @JsonSubTypes.Type(value = MulArithmeticExprImpl.class, name = "mul"),
    @JsonSubTypes.Type(value = NegativeArithmeticExprImpl.class, name = "neg")
})
public abstract class ArithmeticExprMixin extends CommonJsonMixin {
}
