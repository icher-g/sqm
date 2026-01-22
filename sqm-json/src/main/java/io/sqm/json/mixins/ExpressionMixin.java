package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.*;

/* ===========================
 * Top-level: Expression family
 * =========================== */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ArithmeticExpr.class, name = "arithmetic"),
    @JsonSubTypes.Type(value = ParamExpr.class, name = "param"),
    @JsonSubTypes.Type(value = CaseExpr.Impl.class, name = "case"),
    @JsonSubTypes.Type(value = CastExpr.Impl.class, name = "cast"),
    @JsonSubTypes.Type(value = ArrayExpr.Impl.class, name = "array"),
    @JsonSubTypes.Type(value = ColumnExpr.Impl.class, name = "column"),
    @JsonSubTypes.Type(value = FunctionExpr.Impl.class, name = "function"),
    @JsonSubTypes.Type(value = LiteralExpr.Impl.class, name = "literal"),
    @JsonSubTypes.Type(value = BinaryOperatorExpr.Impl.class, name = "binary-op"),
    @JsonSubTypes.Type(value = UnaryOperatorExpr.Impl.class, name = "unary-op"),
})
public abstract class ExpressionMixin extends CommonJsonMixin {
}
