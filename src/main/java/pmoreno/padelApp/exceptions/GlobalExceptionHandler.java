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
}