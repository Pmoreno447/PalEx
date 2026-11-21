package pmoreno.padelApp.dto.Inscription;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

// Solo el torneo: el usuario sale del token, como en las reservas
public record TournamentInscriptionRequest(
    @NotNull(message = "El torneo es obligatorio")
    UUID tournamentId
) {}
