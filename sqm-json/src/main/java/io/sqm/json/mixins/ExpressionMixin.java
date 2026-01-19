package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.ArithmeticExpr;
import io.sqm.core.ArrayExpr;
import io.sqm.core.BinaryOperatorExpr;
import io.sqm.core.CastExpr;
import io.sqm.core.ParamExpr;
import io.sqm.core.UnaryOperatorExpr;
import io.sqm.core.internal.CaseExprImpl;
import io.sqm.core.internal.ColumnExprImpl;
import io.sqm.core.internal.FunctionExprImpl;
import io.sqm.core.internal.LiteralExprImpl;

/* ===========================
 * Top-level: Expression family
 * =========================== */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ArithmeticExpr.class, name = "arithmetic"),
    @JsonSubTypes.Type(value = ParamExpr.class, name = "param"),
    @JsonSubTypes.Type(value = CaseExprImpl.class, name = "case"),
    @JsonSubTypes.Type(value = CastExpr.Impl.class, name = "cast"),
    @JsonSubTypes.Type(value = ArrayExpr.Impl.class, name = "array"),
    @JsonSubTypes.Type(value = ColumnExprImpl.class, name = "column"),
    @JsonSubTypes.Type(value = FunctionExprImpl.class, name = "function"),
    @JsonSubTypes.Type(value = LiteralExprImpl.class, name = "literal"),
    @JsonSubTypes.Type(value = BinaryOperatorExpr.Impl.class, name = "binary-op"),
    @JsonSubTypes.Type(value = UnaryOperatorExpr.Impl.class, name = "unary-op"),
})
public abstract class ExpressionMixin extends CommonJsonMixin {
}
