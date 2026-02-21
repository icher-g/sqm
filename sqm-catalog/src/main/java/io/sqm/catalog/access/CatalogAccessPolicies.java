package io.sqm.catalog.access;

/**
 * Factory methods for common {@link CatalogAccessPolicy} variants.
 */
public final class CatalogAccessPolicies {
    private CatalogAccessPolicies() {
    }

    /**
     * Returns allow-all policy.
     *
     * @return allow-all catalog access policy.
     */
    public static CatalogAccessPolicy allowAll() {
        return DefaultCatalogAccessPolicy.allowAll();
    }
}

