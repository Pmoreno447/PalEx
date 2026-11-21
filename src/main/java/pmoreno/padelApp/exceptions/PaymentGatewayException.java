package pmoreno.padelApp.exceptions;

// Fallo al comunicarse con la pasarela de pago: el error es externo, no de la petición del cliente
public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
