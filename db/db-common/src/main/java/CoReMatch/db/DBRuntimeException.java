package CoReMatch.db;

public class DBRuntimeException extends RuntimeException {

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    public DBRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public DBRuntimeException(String message) {
        super(message);
    }

}
