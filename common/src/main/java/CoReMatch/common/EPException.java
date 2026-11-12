package CoReMatch.common;

public class EPException extends Exception {

    private static final long serialVersionUID = 1L;

    public EPException(String message, Throwable cause) {
        super(message, cause);
    }

    public EPException(String message) {
        super(message);
    }
}
