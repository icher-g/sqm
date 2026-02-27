package io.sqm.validate.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.sqm.catalog.access.DefaultCatalogAccessPolicy;

import java.util.LinkedHashSet;
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
        builder.tenant(config.tenant);

        if (config.tenantRequirementMode != null && !config.tenantRequirementMode.isBlank()) {
            builder.tenantRequirementMode(TenantRequirementMode.valueOf(config.tenantRequirementMode.trim().toUpperCase()));
        }

        if (config.accessPolicy != null) {
            builder.accessPolicy(toAccessPolicy(config.accessPolicy));
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

    private static List<TenantPolicyConfig> safeTenantRules(List<TenantPolicyConfig> values) {
        return values == null ? List.of() : values;
    }

    private static DefaultCatalogAccessPolicy toAccessPolicy(AccessPolicySection config) {
        var policyBuilder = DefaultCatalogAccessPolicy.builder();
        for (var table : safe(config.deniedTables())) {
            policyBuilder.denyTable(table);
        }
        for (var column : safe(config.deniedColumns())) {
            policyBuilder.denyColumn(column);
        }
        for (var functionName : safe(config.allowedFunctions())) {
            policyBuilder.allowFunction(functionName);
        }
        for (var principalRule : safePrincipalRules(config.principals())) {
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
        var tenantNames = new LinkedHashSet<String>();
        for (var tenantRule : safeTenantRules(config.tenants())) {
            var tenant = normalizeTenantName(tenantRule.name);
            if (!tenantNames.add(tenant)) {
                throw new IllegalArgumentException("tenant access policy already defined for tenant: " + tenant);
            }
            for (var table : safe(tenantRule.deniedTables)) {
                policyBuilder.denyTableForTenant(tenant, table);
            }
            for (var column : safe(tenantRule.deniedColumns)) {
                policyBuilder.denyColumnForTenant(tenant, column);
            }
            for (var functionName : safe(tenantRule.allowedFunctions)) {
                policyBuilder.allowFunctionForTenant(tenant, functionName);
            }
            for (var principalRule : safePrincipalRules(tenantRule.principals)) {
                for (var table : safe(principalRule.deniedTables)) {
                    policyBuilder.denyTableForTenantPrincipal(tenant, principalRule.name, table);
                }
                for (var column : safe(principalRule.deniedColumns)) {
                    policyBuilder.denyColumnForTenantPrincipal(tenant, principalRule.name, column);
                }
                for (var functionName : safe(principalRule.allowedFunctions)) {
                    policyBuilder.allowFunctionForTenantPrincipal(tenant, principalRule.name, functionName);
                }
            }
        }
        return policyBuilder.build();
    }

    @JsonIgnoreProperties()
    private static final class SettingsConfig {
        public String principal;
        public String tenant;
        public String tenantRequirementMode;
        public AccessPolicyConfig accessPolicy;
        public LimitsConfig limits;
    }

    @JsonIgnoreProperties()
    private static final class AccessPolicyConfig implements AccessPolicySection {
        public List<String> deniedTables;
        public List<String> deniedColumns;
        public List<String> allowedFunctions;
        public List<PrincipalPolicyConfig> principals;
        public List<TenantPolicyConfig> tenants;

        @Override
        public List<String> deniedTables() {
            return deniedTables;
        }

        @Override
        public List<String> deniedColumns() {
            return deniedColumns;
        }

        @Override
        public List<String> allowedFunctions() {
            return allowedFunctions;
        }

        @Override
        public List<PrincipalPolicyConfig> principals() {
            return principals;
        }

        @Override
        public List<TenantPolicyConfig> tenants() {
            return tenants;
        }
    }

    private interface AccessPolicySection {
        List<String> deniedTables();
        List<String> deniedColumns();
        List<String> allowedFunctions();
        List<PrincipalPolicyConfig> principals();
        List<TenantPolicyConfig> tenants();
    }

    @JsonIgnoreProperties()
    private static final class PrincipalPolicyConfig {
        public String name;
        public List<String> deniedTables;
        public List<String> deniedColumns;
        public List<String> allowedFunctions;
    }

    @JsonIgnoreProperties()
    private static final class TenantPolicyConfig implements AccessPolicySection {
        public String name;
        public List<String> deniedTables;
        public List<String> deniedColumns;
        public List<String> allowedFunctions;
        public List<PrincipalPolicyConfig> principals;

        @Override
        public List<String> deniedTables() {
            return deniedTables;
        }

        @Override
        public List<String> deniedColumns() {
            return deniedColumns;
        }

        @Override
        public List<String> allowedFunctions() {
            return allowedFunctions;
        }

        @Override
        public List<PrincipalPolicyConfig> principals() {
            return principals;
        }

        @Override
        public List<TenantPolicyConfig> tenants() {
            return List.of();
        }
    }

    @JsonIgnoreProperties()
    private static final class LimitsConfig {
        public Integer maxJoinCount;
        public Integer maxSelectColumns;
    }

    private static String normalizeTenantName(String tenant) {
        if (tenant == null || tenant.isBlank()) {
            throw new IllegalArgumentException("tenant name must not be blank");
        }
        return tenant;
    }
}
