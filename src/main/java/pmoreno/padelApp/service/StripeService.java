package pmoreno.padelApp.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.StripeClient;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.RefundListParams;
import com.stripe.param.checkout.SessionCreateParams;

import pmoreno.padelApp.exceptions.PaymentGatewayException;
import pmoreno.padelApp.model.Booking;
import pmoreno.padelApp.model.TournamentRegistration;

@Service 
public class StripeService {
    public static final String PAYMENT_TYPE_KEY = "type";
    public static final String PAYMENT_TYPE_BOOKING = "booking";
    public static final String PAYMENT_TYPE_TOURNAMENT = "tournament";
    private final String frontendUrl;
    private final int paymentMinutes;
    private final StripeClient stripeClient;

    public StripeService(StripeClient stripeClient,
                         @Value ("${app.frontend-url}") String frontendUrl, 
                         @Value("${app.booking.payment-minutes}") int paymentMinutes){
        this.frontendUrl = frontendUrl;
        this.paymentMinutes = paymentMinutes;
        this.stripeClient = stripeClient;
    }

    public Session bookingPayment(Booking booking){
        String bookingUrl = frontendUrl + "/bookings/" + booking.getPublicId();

        SessionCreateParams params =
            SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(bookingUrl)
                // Plazo para pagar: al vencer, Stripe manda checkout.session.expired y la reserva libera el turno (HU-12).
                // Stripe solo admite entre 30 min y 24 h; sin fijarlo serían 24 h con el turno bloqueado
                .setExpiresAt(Instant.now().plus(paymentMinutes, ChronoUnit.MINUTES).getEpochSecond())
                // Stripe lo devuelve tal cual en los eventos del webhook: así se sabe a qué reserva corresponde
                .setClientReferenceId(booking.getPublicId().toString())
                // Marca la sesión como de una reserva: el webhook recibe todas las sesiones de la cuenta y así distingue las nuestras
                .putMetadata(PAYMENT_TYPE_KEY, PAYMENT_TYPE_BOOKING)
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(
                            SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("eur")
                                .setUnitAmount(booking.getAmount().movePointRight(2).longValueExact()) // precio en cents
                                .setProductData(
                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Reserva " + booking.getCourt().getName())
                                        .build())   // ProductData
                                .build())           // PriceData
                        .build())                   // LineItem
                .build();                           // SessionCreateParams

        
        // Se relanza como RuntimeException para que @Transactional deshaga la reserva si Stripe falla
        Session session;
        try {
            session = stripeClient.checkout().sessions().create(params);
        } catch (StripeException e) {
            throw new PaymentGatewayException("No se pudo iniciar el pago", e);
        }

        return session;
    }

    public Session tournamentPayment(TournamentRegistration tournamentRegistration){
        String tournamentUrl = frontendUrl + "/inscription/" + tournamentRegistration.getPublicId();

        SessionCreateParams params =
            SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(tournamentUrl)
                .setExpiresAt(Instant.now().plus(paymentMinutes, ChronoUnit.MINUTES).getEpochSecond())
                .setClientReferenceId(tournamentRegistration.getPublicId().toString())
                // Marca la sesión como de una inscripción a un torneo: el webhook recibe todas las sesiones de la cuenta y así distingue las nuestras
                .putMetadata(PAYMENT_TYPE_KEY, PAYMENT_TYPE_TOURNAMENT)
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(
                            SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("eur")
                            .setUnitAmount(tournamentRegistration.getAmount().movePointRight(2).longValueExact())
                            .setProductData(
                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName("Inscripción en " + tournamentRegistration.getTournament().getName())
                                    .build())
                            .build())
                    .build())
            .build();

        // Se relanza como RuntimeException para que @Transactional deshaga la reserva si Stripe falla
        Session session;
        try {
            session = stripeClient.checkout().sessions().create(params);
        } catch (StripeException e) {
            throw new PaymentGatewayException("No se pudo iniciar el pago", e);
        }

        return session;
    }


    public Refund tournamentRefund(TournamentRegistration tournamentRegistration){
        // Lo que se guarda al pagar es el PaymentIntent (pi_...), no el cargo (ch_...)
        RefundCreateParams params =
            RefundCreateParams.builder()
                .setPaymentIntent(tournamentRegistration.getPaymentIntentId())
                .build();

        // La clave cubre dos cancelaciones simultáneas: Stripe solo ejecuta una
        RequestOptions options =
            RequestOptions.builder()
                .setIdempotencyKey("refund" + tournamentRegistration.getPublicId())
                .build();

        Refund refund;
        try {
            // Stripe olvida la clave a las 24 h: si ya hay una devolución hecha o en curso, no se crea otra
            Optional<Refund> existing = stripeClient.refunds().list(
                    RefundListParams.builder()
                        .setPaymentIntent(tournamentRegistration.getPaymentIntentId())
                        .build())
                .getData().stream()
                .filter(r -> "succeeded".equals(r.getStatus()) || "pending".equals(r.getStatus()))
                .findFirst();
            if(existing.isPresent()){
                return existing.get();
            }

            refund = stripeClient.refunds().create(params, options);
        } catch (StripeException e) {
            throw new PaymentGatewayException("No se pudo iniciar la devolución", e);
        }

        return refund;
    }
}