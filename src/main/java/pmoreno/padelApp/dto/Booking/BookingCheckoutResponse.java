package pmoreno.padelApp.dto.Booking;

import java.util.UUID;

// Respuesta al crear una reserva: el frontend solo necesita a dónde redirigir para pagar
public record BookingCheckoutResponse(
    UUID id,
    String checkoutUrl
) {}
