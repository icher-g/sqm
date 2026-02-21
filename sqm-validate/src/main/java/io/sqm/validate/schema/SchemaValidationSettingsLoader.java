package io.sqm.validate.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.sqm.catalog.access.DefaultCatalogAccessPolicy;

import java.util.List;
import java.util.Objects;

/**
 * Loads {@link SchemaValidationSettings} from JSON or YAML configuration.
 */
public final class SchemaValidationSettingsLoader {
    private static final ObjectMapper JSON_MAPPER = mapper(new ObjectMapper());
    private static final ObjectMapper YAML_MAPPER = mapper(new ObjectMapper(new YAMLFactory()));

    private SchemaValidationSettingsLoader() {
    }

    /**
     * Loads settings from JSON text.
     *
     * @param json JSON configuration text.
     * @return compiled settings.
     */
    public static SchemaValidationSettings fromJson(String json) {
        return load(json, JSON_MAPPER, "JSON");
    }

    /**
     * Loads settings from YAML text.
     *
     * @param yaml YAML configuration text.
     * @return compiled settings.
     */
    public static SchemaValidationSettings fromYaml(String yaml) {
        return load(yaml, YAML_MAPPER, "YAML");
    }

    private static ObjectMapper mapper(ObjectMapper mapper) {
        mapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        return mapper;
    }

    private static SchemaValidationSettings load(String source, ObjectMapper mapper, String format) {
        Objects.requireNonNull(source, "source");
        try {
            var config = mapper.readValue(source, SettingsConfig.class);
            return toSettings(config);
        } catch (Exception ex) {
            throw new IllegalArgumentException(format + " policy config is invalid: " + ex.getMessage(), ex);
        }
    }

    private static SchemaValidationSettings toSettings(SettingsConfig config) {
        var builder = SchemaValidationSettings.builder();
        builder.principal(config.principal);

        if (config.accessPolicy != null) {
            var policyBuilder = DefaultCatalogAccessPolicy.builder();
            for (var table : safe(config.accessPolicy.deniedTables)) {
                policyBuilder.denyTable(table);
            }
            for (var column : safe(config.accessPolicy.deniedColumns)) {
                policyBuilder.denyColumn(column);
            }
            for (var functionName : safe(config.accessPolicy.allowedFunctions)) {
                policyBuilder.allowFunction(functionName);
            }
            for (var principalRule : safePrincipalRules(config.accessPolicy.principals)) {
                for (var table : safe(principalRule.deniedTables)) {
                    policyBuilder.denyTableForPrincipal(principalRule.name, table);
                }
                for (var column : safe(principalRule.deniedColumns)) {
                    policyBuilder.denyColumnForPrincipal(principalRule.name, column);
                }
                for (var functionName : safe(principalRule.allowedFunctions)) {
                    policyBuilder.allowFunctionForPrincipal(principalRule.name, functionName);
                }
            }
            builder.accessPolicy(policyBuilder.build());
        }

        if (config.limits != null) {
            var limitsBuilder = SchemaValidationLimits.builder();
            if (config.limits.maxJoinCount != null) {
                limitsBuilder.maxJoinCount(config.limits.maxJoinCount);
            }
            if (config.limits.maxSelectColumns != null) {
                limitsBuilder.maxSelectColumns(config.limits.maxSelectColumns);
            }
            builder.limits(limitsBuilder.build());
        }

        return builder.build();
    }

    private static List<String> safe(List<String> values) {
        return values == null ? List.of() : values;
    }

    private static List<PrincipalPolicyConfig> safePrincipalRules(List<PrincipalPolicyConfig> values) {
        return values == null ? List.of() : values;
    }

    @JsonIgnoreProperties()
    private static final class SettingsConfig {
        public String principal;
        public AccessPolicyConfig accessPolicy;
        public LimitsConfig limits;
    }

    @JsonIgnoreProperties()
    private static final class AccessPolicyConfig {
        public List<String> deniedTables;
        public List<String> deniedColumns;
        public List<String> allowedFunctions;
        public List<PrincipalPolicyConfig> principals;
    }

    @JsonIgnoreProperties()
    private static final class PrincipalPolicyConfig {
        public String name;
        public List<String> deniedTables;
        public List<String> deniedColumns;
        public List<String> allowedFunctions;
    }

    @JsonIgnoreProperties()
    private static final class LimitsConfig {
        public Integer maxJoinCount;
        public Integer maxSelectColumns;
    }
}
