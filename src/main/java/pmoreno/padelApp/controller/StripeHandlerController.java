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
import pmoreno.padelApp.service.StripeService;
import pmoreno.padelApp.service.TournamentInscriptionService;

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
    private final TournamentInscriptionService tournamentInscriptionService;
    private final StripeClient stripeClient;
    private final String endpointSecret;

    public StripeHandlerController(@Value("${stripe.secret-endpoint-key}") String endpointSecret, StripeClient stripeClient,
                                   BookingService bookingService, TournamentInscriptionService tournamentInscriptionService){
        this.bookingService = bookingService;
        this.tournamentInscriptionService = tournamentInscriptionService;
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
                String typeConfirmed = paymentType(sessionConfirmed);
                String paymentIntentId = sessionConfirmed.getPaymentIntent();
                if(StripeService.PAYMENT_TYPE_BOOKING.equals(typeConfirmed)){
                    bookingService.confirmBooking(referenceId(sessionConfirmed));
                } else if(StripeService.PAYMENT_TYPE_TOURNAMENT.equals(typeConfirmed)){
                    tournamentInscriptionService.confirmInscription(referenceId(sessionConfirmed), paymentIntentId);
                }
                break;
            case "checkout.session.expired":
                Session sessionExpired = (Session) stripeObject;
                String typeExpired = paymentType(sessionExpired);
                if(StripeService.PAYMENT_TYPE_BOOKING.equals(typeExpired)){
                    bookingService.expireBooking(referenceId(sessionExpired));
                } else if(StripeService.PAYMENT_TYPE_TOURNAMENT.equals(typeExpired)){
                    tournamentInscriptionService.expireInscription(referenceId(sessionExpired));
                }
                break;
            default:
                break;
        }
        // Stripe solo necesita un 2xx para dar el evento por entregado; si no, lo reintenta
        return ResponseEntity.ok().build();
    }

    // Qué se ha pagado, según la marca que pone StripeService al crear la sesión. Las sesiones sin marca
    // (otra app de la cuenta, un Payment Link...) devuelven null y no entran en ningún if: se ignoran con 200
    private String paymentType(Session session){
        return session.getMetadata() == null ? null : session.getMetadata().get(StripeService.PAYMENT_TYPE_KEY);
    }

    // El publicId de la reserva o la inscripción, que viaja como client_reference_id
    private UUID referenceId(Session session){
        return UUID.fromString(session.getClientReferenceId());
    }
    
}
