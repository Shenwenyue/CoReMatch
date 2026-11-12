package CoReMatch.db;

public class DBException extends Exception {

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    public DBException(String message) {
        super(message);
    }

    public DBException(String message, Throwable cause) {
        super(message, cause);
    }

}
