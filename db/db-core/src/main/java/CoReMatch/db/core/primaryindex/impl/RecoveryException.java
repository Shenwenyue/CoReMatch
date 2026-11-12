package CoReMatch.db.core.primaryindex.impl;

import CoReMatch.db.DBException;

public class RecoveryException extends DBException {

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    public RecoveryException(String message, Throwable cause) {
        super(message, cause);
    }

    public RecoveryException(String message) {
        super(message);
    }
}
