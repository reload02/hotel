package common;

public class FatalDataException extends RuntimeException {
    public FatalDataException(String message) {
        super(message);
    }

    public FatalDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
