package org.hyperledger.aries.askar;

/**
 * Methods for deriving keys from seeds.
 */
public enum SeedMethod {
    /**
     * Blake2b key derivation.
     */
    BLAKE2B("blake2b"),

    /**
     * Raw seed (no derivation).
     */
    RAW("raw");

    private final String methodName;

    SeedMethod(String methodName) {
        this.methodName = methodName;
    }

    /**
     * Get the method name as used by the native library.
     * @return The method name
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Parse method from string.
     * @param method The method string
     * @return The SeedMethod enum value
     * @throws IllegalArgumentException if method is not supported
     */
    public static SeedMethod fromString(String method) {
        if (method == null) {
            return BLAKE2B; // Default
        }

        for (SeedMethod m : values()) {
            if (m.methodName.equalsIgnoreCase(method)) {
                return m;
            }
        }

        throw new IllegalArgumentException("Unsupported seed method: " + method);
    }

    @Override
    public String toString() {
        return methodName;
    }
}