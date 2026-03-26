package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.*;

/**
 * Jackson mixin root for SQM node polymorphism.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TypeName.Impl.class, name = "typeName"),
    @JsonSubTypes.Type(value = DistinctSpec.Impl.class, name = "distinctSpec"),
    @JsonSubTypes.Type(value = GroupBy.Impl.class, name = "groupBy"),
    @JsonSubTypes.Type(value = GroupItem.SimpleGroupItemImpl.class, name = "group"),
    @JsonSubTypes.Type(value = GroupItem.GroupingSetImpl.class, name = "groupingSet"),
    @JsonSubTypes.Type(value = GroupItem.GroupingSetsImpl.class, name = "groupingSets"),
    @JsonSubTypes.Type(value = GroupItem.RollupImpl.class, name = "rollup"),
    @JsonSubTypes.Type(value = GroupItem.CubeImpl.class, name = "cube"),
    @JsonSubTypes.Type(value = OrderBy.Impl.class, name = "orderBy"),
    @JsonSubTypes.Type(value = OrderItem.Impl.class, name = "order"),
    @JsonSubTypes.Type(value = CteDef.Impl.class, name = "cte"),
    @JsonSubTypes.Type(value = WhenThen.Impl.class, name = "whenThen"),
    @JsonSubTypes.Type(value = TopSpec.Impl.class, name = "topSpec"),
    @JsonSubTypes.Type(value = MergeClause.Impl.class, name = "mergeClause"),
    @JsonSubTypes.Type(value = MergeUpdateAction.Impl.class, name = "mergeUpdateAction"),
    @JsonSubTypes.Type(value = MergeDeleteAction.Impl.class, name = "mergeDeleteAction"),
    @JsonSubTypes.Type(value = MergeInsertAction.Impl.class, name = "mergeInsertAction"),
    @JsonSubTypes.Type(value = StatementHint.Impl.class, name = "statementHint"),
    @JsonSubTypes.Type(value = TableHint.Impl.class, name = "tableHint"),
    @JsonSubTypes.Type(value = IdentifierHintArg.Impl.class, name = "identifierHintArg"),
    @JsonSubTypes.Type(value = QualifiedNameHintArg.Impl.class, name = "qualifiedNameHintArg"),
    @JsonSubTypes.Type(value = ExpressionHintArg.Impl.class, name = "expressionHintArg"),
    @JsonSubTypes.Type(value = ResultClause.Impl.class, name = "resultClause"),
    @JsonSubTypes.Type(value = ResultInto.Impl.class, name = "resultInto"),
    @JsonSubTypes.Type(value = LimitOffset.Impl.class, name = "limitOffset"),
    @JsonSubTypes.Type(value = Assignment.Impl.class, name = "assignment"),
    @JsonSubTypes.Type(value = WindowDef.Impl.class, name = "window"),
    @JsonSubTypes.Type(value = PartitionBy.Impl.class, name = "partitionBy"),
    @JsonSubTypes.Type(value = LockingClause.Impl.class, name = "lockFor")
})
public abstract class NodeMixin extends CommonJsonMixin {

    /**
     * Creates node mixin metadata.
     */
    protected NodeMixin() {
    }
}
