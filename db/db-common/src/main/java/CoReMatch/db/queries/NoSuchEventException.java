package CoReMatch.db.queries;

import CoReMatch.db.DBException;

public class NoSuchEventException extends DBException {

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    public NoSuchEventException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    public NoSuchEventException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

}
