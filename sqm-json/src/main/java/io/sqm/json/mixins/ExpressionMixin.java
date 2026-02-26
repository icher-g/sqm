package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.*;

/* ===========================
 * Top-level: Expression family
 * =========================== */

/**
 * Jackson mixin root for expression polymorphism.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ArithmeticExpr.class, name = "arithmetic"),
    @JsonSubTypes.Type(value = ParamExpr.class, name = "param"),
    @JsonSubTypes.Type(value = CaseExpr.Impl.class, name = "case"),
    @JsonSubTypes.Type(value = CastExpr.Impl.class, name = "cast"),
    @JsonSubTypes.Type(value = ArrayExpr.Impl.class, name = "array"),
    @JsonSubTypes.Type(value = ArraySubscriptExpr.Impl.class, name = "array-subscript"),
    @JsonSubTypes.Type(value = ArraySliceExpr.Impl.class, name = "array-slice"),
    @JsonSubTypes.Type(value = CollateExpr.Impl.class, name = "collate"),
    @JsonSubTypes.Type(value = ColumnExpr.Impl.class, name = "column"),
    @JsonSubTypes.Type(value = FunctionExpr.Impl.class, name = "function"),
    @JsonSubTypes.Type(value = LiteralExpr.Impl.class, name = "literal"),
    @JsonSubTypes.Type(value = DateLiteralExpr.Impl.class, name = "date-literal"),
    @JsonSubTypes.Type(value = TimeLiteralExpr.Impl.class, name = "time-literal"),
    @JsonSubTypes.Type(value = TimestampLiteralExpr.Impl.class, name = "timestamp-literal"),
    @JsonSubTypes.Type(value = IntervalLiteralExpr.Impl.class, name = "interval-literal"),
    @JsonSubTypes.Type(value = BitStringLiteralExpr.Impl.class, name = "bit-string-literal"),
    @JsonSubTypes.Type(value = HexStringLiteralExpr.Impl.class, name = "hex-string-literal"),
    @JsonSubTypes.Type(value = EscapeStringLiteralExpr.Impl.class, name = "escape-string-literal"),
    @JsonSubTypes.Type(value = DollarStringLiteralExpr.Impl.class, name = "dollar-string-literal"),
    @JsonSubTypes.Type(value = BinaryOperatorExpr.Impl.class, name = "binary-op"),
    @JsonSubTypes.Type(value = UnaryOperatorExpr.Impl.class, name = "unary-op"),
})
public abstract class ExpressionMixin extends CommonJsonMixin {

    /**
     * Creates expression mixin metadata.
     */
    protected ExpressionMixin() {
    }
}
