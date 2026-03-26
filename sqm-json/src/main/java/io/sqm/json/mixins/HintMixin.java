package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.StatementHint;
import io.sqm.core.TableHint;

/**
 * Jackson mixin root for typed hint polymorphism.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = StatementHint.Impl.class, name = "statement_hint"),
    @JsonSubTypes.Type(value = TableHint.Impl.class, name = "table_hint")
})
public abstract class HintMixin extends CommonJsonMixin {

    /**
     * Creates hint mixin metadata.
     */
    protected HintMixin() {
    }
}