package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.TypeName;
import io.sqm.core.internal.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TypeName.Impl.class, name = "typeName"),
    @JsonSubTypes.Type(value = DistinctSpecImpl.class, name = "distinctSpec"),
    @JsonSubTypes.Type(value = GroupByImpl.class, name = "groupBy"),
    @JsonSubTypes.Type(value = GroupItemImpl.class, name = "group"),
    @JsonSubTypes.Type(value = OrderByImpl.class, name = "orderBy"),
    @JsonSubTypes.Type(value = OrderItemImpl.class, name = "order"),
    @JsonSubTypes.Type(value = CteDefImpl.class, name = "cte"),
    @JsonSubTypes.Type(value = WhenThenImpl.class, name = "whenThen"),
    @JsonSubTypes.Type(value = LimitOffsetImpl.class, name = "limitOffset"),
    @JsonSubTypes.Type(value = WindowDefImpl.class, name = "window"),
    @JsonSubTypes.Type(value = PartitionByImpl.class, name = "partitionBy"),
})
public abstract class NodeMixin extends CommonJsonMixin {
}
