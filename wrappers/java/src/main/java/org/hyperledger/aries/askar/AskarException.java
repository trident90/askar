package org.hyperledger.aries.askar;

/**
 * Exception thrown by Askar operations.
 */
public class AskarException extends Exception {

    /**
     * Askar error codes corresponding to Rust AskarErrorCode enum.
     */
    public enum ErrorCode {
        SUCCESS(0),
        BACKEND(1),
        BUSY(2),
        DUPLICATE(3),
        ENCRYPTION(4),
        INPUT(5),
        NOT_FOUND(6),
        UNEXPECTED(7),
        UNSUPPORTED(8),
        WRAPPER(100);

        private final int code;

        ErrorCode(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static ErrorCode fromCode(int code) {
            for (ErrorCode errorCode : values()) {
                if (errorCode.code == code) {
                    return errorCode;
                }
            }
            return UNEXPECTED;
        }
    }

    private final ErrorCode errorCode;
    private final Object extra;

    public AskarException(ErrorCode errorCode, String message) {
        this(errorCode, message, null, null);
    }

    public AskarException(ErrorCode errorCode, String message, Object extra) {
        this(errorCode, message, extra, null);
    }

    public AskarException(ErrorCode errorCode, String message, Throwable cause) {
        this(errorCode, message, null, cause);
    }

    public AskarException(ErrorCode errorCode, String message, Object extra, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.extra = extra;
    }

    /**
     * Get the error code.
     * @return The error code
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Get extra error information if available.
     * @return Extra error information or null
     */
    public Object getExtra() {
        return extra;
    }

    @Override
    public String toString() {
        return String.format("AskarException[%s]: %s", errorCode, getMessage());
    }
}