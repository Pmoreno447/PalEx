package pmoreno.padelApp.dto.Inscription;

import java.util.UUID;

// Respuesta al inscribirse en un torneo: como en las reservas, el frontend solo necesita a dónde redirigir para pagar
public record TournamentInscriptionResponse(
    UUID id,
    String checkoutUrl
) {}
