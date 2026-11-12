package CoReMatch.db.core.primaryindex.impl;

import CoReMatch.db.DBException;

public class InitialializationException extends DBException {

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    public InitialializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public InitialializationException(String message) {
        super(message);
    }
}
