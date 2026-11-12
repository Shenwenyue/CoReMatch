package CoReMatch.esp.ql;

import CoReMatch.common.EPException;

public class TranslatorException extends EPException {

    private static final long serialVersionUID = 1L;

    public TranslatorException(String message) {
        super(message);
    }

    public TranslatorException(String message, Throwable cause) {
        super(message, cause);
    }

}
