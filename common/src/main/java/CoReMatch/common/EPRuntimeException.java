package CoReMatch.common;

public class EPRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EPRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public EPRuntimeException(String message) {
        super(message);
    }
}
