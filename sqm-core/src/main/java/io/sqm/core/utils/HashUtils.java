package io.sqm.core.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility hashing helpers.
 */
public final class HashUtils {
    private HashUtils() {
    }

    /**
     * Computes SHA-256 hash and returns lowercase hex string.
     *
     * @param input bytes to hash.
     * @return SHA-256 hex.
     */
    public static String sha256Hex(byte[] input) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
