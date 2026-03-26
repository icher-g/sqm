package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.ExpressionHintArg;
import io.sqm.core.IdentifierHintArg;
import io.sqm.core.QualifiedNameHintArg;

/**
 * Jackson mixin root for typed hint-argument polymorphism.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = IdentifierHintArg.Impl.class, name = "identifier_hint_arg"),
    @JsonSubTypes.Type(value = QualifiedNameHintArg.Impl.class, name = "qualified_name_hint_arg"),
    @JsonSubTypes.Type(value = ExpressionHintArg.Impl.class, name = "expression_hint_arg")
})
public abstract class HintArgMixin extends CommonJsonMixin {

    /**
     * Creates hint-argument mixin metadata.
     */
    protected HintArgMixin() {
    }
}