package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.internal.CaseExprImpl;
import io.sqm.core.internal.ColumnExprImpl;
import io.sqm.core.internal.FunctionExprImpl;
import io.sqm.core.internal.LiteralExprImpl;

/* ===========================
 * Top-level: Expression family
 * =========================== */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CaseExprImpl.class, name = "case"),
    @JsonSubTypes.Type(value = ColumnExprImpl.class, name = "column"),
    @JsonSubTypes.Type(value = FunctionExprImpl.class, name = "function"),
    @JsonSubTypes.Type(value = LiteralExprImpl.class, name = "literal"),
})
public abstract class ExpressionMixin extends CommonJsonMixin {
}
