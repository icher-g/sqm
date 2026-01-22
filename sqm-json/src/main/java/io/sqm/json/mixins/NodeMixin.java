package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TypeName.Impl.class, name = "typeName"),
    @JsonSubTypes.Type(value = DistinctSpec.Impl.class, name = "distinctSpec"),
    @JsonSubTypes.Type(value = GroupBy.Impl.class, name = "groupBy"),
    @JsonSubTypes.Type(value = GroupItem.Impl.class, name = "group"),
    @JsonSubTypes.Type(value = OrderBy.Impl.class, name = "orderBy"),
    @JsonSubTypes.Type(value = OrderItem.Impl.class, name = "order"),
    @JsonSubTypes.Type(value = CteDef.Impl.class, name = "cte"),
    @JsonSubTypes.Type(value = WhenThen.Impl.class, name = "whenThen"),
    @JsonSubTypes.Type(value = LimitOffset.Impl.class, name = "limitOffset"),
    @JsonSubTypes.Type(value = WindowDef.Impl.class, name = "window"),
    @JsonSubTypes.Type(value = PartitionBy.Impl.class, name = "partitionBy"),
})
public abstract class NodeMixin extends CommonJsonMixin {
}
