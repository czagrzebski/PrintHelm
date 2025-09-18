package com.czagrzebski.printhelm.web.exception;

public class PrintHelmException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PrintHelmException(String message) {
        super(message);
    }

    public PrintHelmException(String message, Throwable cause) {
        super(message, cause);
    }

    public PrintHelmException(Throwable cause) {
        super(cause);
    }
}
