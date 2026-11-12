package CoReMatch.db.core.primaryindex.impl;

import CoReMatch.db.DBException;

public class LoadException extends DBException {

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    public LoadException(String message, Throwable cause) {
        super(message, cause);
    }

    public LoadException(String message) {
        super(message);
    }
}
