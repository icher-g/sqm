package io.sqm.playground.rest.service;

import io.sqm.core.*;
import io.sqm.playground.api.AstChildSlotDto;
import io.sqm.playground.api.AstDetailDto;
import io.sqm.playground.api.AstNodeDto;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Maps SQM model nodes into browser-friendly AST DTOs by reflecting over the SQM interfaces.
 *
 * <p>The mapper treats zero-argument SQM accessors that return {@link Node} values as child slots,
 * and scalar-like accessors as details. This keeps the AST view close to the real SQM model while
 * reducing maintenance when the model evolves.</p>
 */
@Service
public final class SqmAstMapper {

    private static final Map<String, Integer> SLOT_ORDER = Map.ofEntries(
        Map.entry("hints", 0),
        Map.entry("with", 10),
        Map.entry("items", 20),
        Map.entry("from", 30),
        Map.entry("joins", 40),
        Map.entry("where", 50),
        Map.entry("groupBy", 60),
        Map.entry("having", 70),
        Map.entry("orderBy", 80),
        Map.entry("limit", 90),
        Map.entry("offset", 100),
        Map.entry("fetch", 110)
    );

    private static final Set<String> IGNORED_METHODS = Set.of(
        "accept",
        "builder",
        "getTopLevelInterface",
        "matchStatement",
        "matchQuery",
        "matchExpression",
        "matchPredicate",
        "matchSelectItem",
        "matchTableRef",
        "matchJoin",
        "matchGroupItem",
        "matchFromItem"
    );

    private static final Set<String> ALLOWED_DEFAULT_METHODS = Set.of(
        "hints"
    );

    /**
     * Converts an SQM node into an AST node DTO.
     *
     * @param node SQM node
     * @return mapped AST node
     */
    AstNodeDto toAst(Node node) {
        return toAst(node, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private AstNodeDto toAst(Node node, Set<Node> path) {
        Objects.requireNonNull(node, "node must not be null");

        var nodeInterface = node.getTopLevelInterface();
        var details = new ArrayList<AstDetailDto>();
        var children = new ArrayList<AstChildSlotDto>();

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
        return Arrays.stream(nodeInterface.getMethods())
            .filter(method -> method.getDeclaringClass() != Object.class)
            .filter(method -> method.getParameterCount() == 0)
            .filter(method -> !method.isDefault() || ALLOWED_DEFAULT_METHODS.contains(method.getName()))
            .filter(method -> !Modifier.isStatic(method.getModifiers()))
            .filter(method -> !method.isSynthetic())
            .filter(method -> !IGNORED_METHODS.contains(method.getName()))
            .sorted(Comparator
                .comparingInt((Method method) -> slotOrder(method.getName()))
                .thenComparing(Method::getName))
            .toList();
    }

    private int slotOrder(String slot) {
        return SLOT_ORDER.getOrDefault(slot, Integer.MAX_VALUE);
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
            return List.of(detail(name, renderIdentifier(identifier)));
        }

        if (value instanceof QualifiedName qualifiedName) {
            return List.of(detail(name, renderQualifiedName(qualifiedName)));
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
            return List.of(detail(name, identifiers.stream().map(this::renderIdentifier).toList().toString()));
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

    private String renderQualifiedName(QualifiedName qualifiedName) {
        return qualifiedName.parts().stream()
            .map(this::renderIdentifier)
            .collect(Collectors.joining("."));
    }

    private String renderIdentifier(Identifier identifier) {
        return switch (identifier.quoteStyle()) {
            case NONE -> identifier.value();
            case DOUBLE_QUOTE -> "\"" + identifier.value() + "\"";
            case BACKTICK -> "`" + identifier.value() + "`";
            case BRACKETS -> "[" + identifier.value() + "]";
        };
    }

    private AstDetailDto detail(String name, String value) {
        return new AstDetailDto(name, value);
    }
}
