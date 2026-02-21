package io.sqm.core.transform;

import io.sqm.core.OrdinalParamExpr;
import io.sqm.core.Query;
import io.sqm.core.utils.HashUtils;
import io.sqm.core.utils.SerializationUtils;

import java.util.Objects;

/**
 * Produces deterministic fingerprints for SQL AST queries.
 *
 * <p>The default fingerprint normalizes literals into ordinal parameters, so queries that
 * differ only by literal values map to the same fingerprint.</p>
 */
public final class QueryFingerprint {

    private QueryFingerprint() {
    }

    /**
     * Computes fingerprint for query using default normalization rules.
     *
     * @param query query to fingerprint.
     * @return SHA-256 fingerprint in lowercase hex.
     */
    public static String of(Query query) {
        return of(query, true);
    }

    /**
     * Computes fingerprint for query with configurable literal normalization.
     *
     * @param query query to fingerprint.
     * @param parameterizeLiterals whether to normalize literals into positional parameters.
     * @return SHA-256 fingerprint in lowercase hex.
     */
    public static String of(Query query, boolean parameterizeLiterals) {
        Objects.requireNonNull(query, "query");
        var normalized = parameterizeLiterals ? normalizeLiterals(query) : query;
        return HashUtils.sha256Hex(SerializationUtils.serialize(normalized));
    }

    /**
     * Returns a normalized query used as fingerprinting input.
     *
     * @param query query to normalize.
     * @return normalized query.
     */
    public static Query normalize(Query query) {
        Objects.requireNonNull(query, "query");
        return normalizeLiterals(query);
    }

    private static Query normalizeLiterals(Query query) {
        var transformer = new ParameterizeLiteralsTransformer(OrdinalParamExpr::of);
        return (Query) transformer.transform(query);
    }
}
