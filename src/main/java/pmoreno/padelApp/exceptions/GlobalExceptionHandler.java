package pmoreno.padelApp.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * GlobalExceptionHandler
 */
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler {
    
    @ExceptionHandler
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException rException){
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, rException.getMessage());
    }

    @ExceptionHandler
    public ProblemDetail handleBadRequest(BadRequestException bException){
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, bException.getMessage());
    }

    @ExceptionHandler 
    public  ProblemDetail handleConflic(ConflictException cException){
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, cException.getMessage());
    }

    // 502: el servidor está bien pero falló un servicio externo. No se expone el mensaje de la pasarela
    @ExceptionHandler
    public ProblemDetail handlePaymentGateway(PaymentGatewayException pException){
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, pException.getMessage());
    }

    @ExceptionHandler
    public ProblemDetail handleSignatureVerificationException(BadSignatureException sException){
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, sException.getMessage());
    }
}