package pmoreno.padelApp.exceptions;

public class BadSignatureException extends RuntimeException {

    public BadSignatureException(String message, Throwable cause){
        super(message, cause);
    }
}
