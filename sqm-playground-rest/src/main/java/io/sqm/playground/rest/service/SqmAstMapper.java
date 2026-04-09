package io.sqm.playground.rest.service;

import io.sqm.core.Identifier;
import io.sqm.core.Node;
import io.sqm.core.Predicate;
import io.sqm.core.Statement;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.sqm.playground.api.AstChildSlotDto;
import io.sqm.playground.api.AstDetailDto;
import io.sqm.playground.api.AstNodeDto;

/**
 * Maps SQM model nodes into browser-friendly AST DTOs by reflecting over the SQM interfaces.
 *
 * <p>The mapper treats zero-argument SQM accessors that return {@link Node} values as child slots,
 * and scalar-like accessors as details. This keeps the AST view close to the real SQM model while
 * reducing maintenance when the model evolves.</p>
 */
@Service
public final class SqmAstMapper {

    private static final Set<String> IGNORED_METHODS = Set.of(
        "accept",
        "getTopLevelInterface",
        "matchStatement",
        "matchQuery",
        "matchExpression",
        "matchPredicate",
        "matchSelectItem",
        "matchTableRef",
        "matchJoin",
        "matchGroupItem"
    );

    /**
     * Converts an SQM node into an AST node DTO.
     *
     * @param node SQM node
     * @return mapped AST node
     */
    AstNodeDto toAst(Node node) {
        return toAst(node, java.util.Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private AstNodeDto toAst(Node node, Set<Node> path) {
        Objects.requireNonNull(node, "node must not be null");

        var nodeInterface = node.getTopLevelInterface();
        var details = new ArrayList<AstDetailDto>();
        var children = new ArrayList<AstChildSlotDto>();

        details.add(detail("interfaceSimpleName", nodeInterface.getSimpleName()));
        if (!node.getClass().equals(nodeInterface)) {
            details.add(detail("implementationClass", node.getClass().getName()));
        }
        if (!path.add(node)) {
            details.add(detail("cycleDetected", "true"));
            return new AstNodeDto(
                nodeInterface.getSimpleName(),
                nodeInterface.getName(),
                lowerCamel(nodeInterface.getSimpleName()),
                categoryFor(node),
                nodeInterface.getSimpleName(),
                List.copyOf(details),
                List.of()
            );
        }

        for (Method method : astMethods(nodeInterface)) {
            var value = invoke(node, method);
            switch (value) {
                case null -> {
                    continue;
                }
                case Node childNode -> {
                    children.add(new AstChildSlotDto(method.getName(), false, List.of(toAst(childNode, path))));
                    continue;
                }
                case List<?> listValue -> {
                    if (listValue.isEmpty()) {
                        continue;
                    }
                    if (listValue.stream().allMatch(Node.class::isInstance)) {
                        children.add(new AstChildSlotDto(
                            method.getName(),
                            true,
                            listValue.stream().map(Node.class::cast).map(child -> toAst(child, path)).toList()
                        ));
                        continue;
                    }
                    details.addAll(detailsForValue(method.getName(), listValue));
                    continue;
                }
                default -> {
                }
            }

            details.addAll(detailsForValue(method.getName(), value));
        }

        path.remove(node);

        return new AstNodeDto(
            nodeInterface.getSimpleName(),
            nodeInterface.getName(),
            lowerCamel(nodeInterface.getSimpleName()),
            categoryFor(node),
            nodeInterface.getSimpleName(),
            List.copyOf(details),
            List.copyOf(children)
        );
    }

    private List<Method> astMethods(Class<? extends Node> nodeInterface) {
        return java.util.Arrays.stream(nodeInterface.getMethods())
            .filter(method -> method.getDeclaringClass() != Object.class)
            .filter(method -> method.getParameterCount() == 0)
            .filter(method -> !method.isDefault())
            .filter(method -> !method.isSynthetic())
            .filter(method -> !IGNORED_METHODS.contains(method.getName()))
            .sorted(Comparator.comparing(Method::getName))
            .toList();
    }

    private Object invoke(Node node, Method method) {
        try {
            return method.invoke(node);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot access SQM accessor " + method.getName() + ".", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("SQM accessor " + method.getName() + " threw an exception.", e.getCause());
        }
    }

    private List<AstDetailDto> detailsForValue(String name, Object value) {
        if (value instanceof Identifier identifier) {
            return List.of(
                detail(name, identifier.value()),
                detail(name + "Quoted", Boolean.toString(identifier.quoted()))
            );
        }

        if (value instanceof Enum<?> enumValue) {
            return List.of(detail(name, enumValue.name()));
        }

        if (value instanceof List<?> listValue) {
            return detailsForList(name, listValue);
        }

        return List.of(detail(name, String.valueOf(value)));
    }

    private List<AstDetailDto> detailsForList(String name, List<?> values) {
        if (values.isEmpty()) {
            return List.of();
        }

        if (values.stream().allMatch(Identifier.class::isInstance)) {
            var identifiers = values.stream().map(Identifier.class::cast).toList();
            return List.of(
                detail(name, identifiers.stream().map(Identifier::value).toList().toString()),
                detail(name + "QuotedCount", Long.toString(identifiers.stream().filter(Identifier::quoted).count()))
            );
        }

        var grouped = new LinkedHashMap<String, Integer>();
        for (Object value : values) {
            grouped.merge(String.valueOf(value), 1, Integer::sum);
        }

        return List.of(
            detail(name + "Count", Integer.toString(values.size())),
            detail(name, grouped.keySet().toString())
        );
    }

    private String categoryFor(Node node) {
        if (node instanceof Statement) {
            return "statement";
        }
        if (node instanceof io.sqm.core.SelectItem) {
            return "selectItem";
        }
        if (node instanceof io.sqm.core.Join) {
            return "join";
        }
        if (node instanceof io.sqm.core.TableRef) {
            return "tableRef";
        }
        if (node instanceof Predicate) {
            return "predicate";
        }
        if (node instanceof io.sqm.core.Expression) {
            return "expression";
        }
        if (node instanceof io.sqm.core.OrderBy) {
            return "orderBy";
        }
        if (node instanceof io.sqm.core.OrderItem) {
            return "orderItem";
        }
        if (node instanceof io.sqm.core.GroupBy) {
            return "groupBy";
        }
        if (node instanceof io.sqm.core.GroupItem) {
            return "groupItem";
        }
        return "node";
    }

    private String lowerCamel(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private AstDetailDto detail(String name, String value) {
        return new AstDetailDto(name, value);
    }
}
