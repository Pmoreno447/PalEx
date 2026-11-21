package pmoreno.padelApp.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;

import pmoreno.padelApp.exceptions.BadSignatureException;
import pmoreno.padelApp.service.BookingService;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;


@RestController 
@RequestMapping("/handler/stripe")
public class StripeHandlerController {
    private static final Logger log = LoggerFactory.getLogger(StripeHandlerController.class);
    private final BookingService bookingService;
    private final StripeClient stripeClient;
    private final String endpointSecret;

    public StripeHandlerController(@Value("${stripe.secret-endpoint-key}") String endpointSecret, StripeClient stripeClient, BookingService bookingService){
        this.bookingService = bookingService;
        this.endpointSecret = endpointSecret;
        this.stripeClient = stripeClient;
    }
    @PostMapping
    public ResponseEntity<Void> stripeHandler(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event = null;
        try {
            event = stripeClient.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            throw new BadSignatureException("La firma del evento es incorrecta", e);
        }

        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();

        // Vacío si la versión de API del evento no coincide con la del SDK. Es fallo nuestro: 500 para que Stripe lo reintente
        if(dataObjectDeserializer.getObject().isEmpty()){
            log.error("No se pudo deserializar el evento {} de tipo {}", event.getId(), event.getType());
            return ResponseEntity.internalServerError().build();
        }
        StripeObject stripeObject = dataObjectDeserializer.getObject().get();

        // Si el service lanza una excepción (p. ej. la BD caída) sube sola como 500 y Stripe reintenta
        switch (event.getType()) {
            case "checkout.session.completed":
                Session sessionConfirmed = (Session) stripeObject;
                if(isBooking(sessionConfirmed)){
                    UUID publicIdConfirm = UUID.fromString(sessionConfirmed.getClientReferenceId());
                    bookingService.confirmBooking(publicIdConfirm);
                }
                break;
            case "checkout.session.expired":
                Session sessionExpired = (Session) stripeObject;
                if(isBooking(sessionExpired)){
                    UUID publicIdExpired = UUID.fromString(sessionExpired.getClientReferenceId());
                    bookingService.expireBooking(publicIdExpired);
                }
                break;
            default:
                break;
        }
        // Stripe solo necesita un 2xx para dar el evento por entregado; si no, lo reintenta
        return ResponseEntity.ok().build();
    }

    // Solo las sesiones creadas al reservar llevan la marca. Las demás de la cuenta (otra app, un Payment Link...) se ignoran con 200
    private boolean isBooking(Session session){
        return session.getMetadata() != null
            && BookingService.PAYMENT_TYPE_BOOKING.equals(session.getMetadata().get(BookingService.PAYMENT_TYPE_KEY));
    }
    
}
