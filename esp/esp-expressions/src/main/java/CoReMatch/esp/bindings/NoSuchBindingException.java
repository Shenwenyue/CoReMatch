package CoReMatch.esp.bindings;

import CoReMatch.common.EPRuntimeException;

public class NoSuchBindingException extends EPRuntimeException {

    private static final long serialVersionUID = 1L;

    public NoSuchBindingException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchBindingException(String message) {
        super(message);
    }
}
