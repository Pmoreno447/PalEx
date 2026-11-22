package pmoreno.padelApp.dto.Tournament;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import pmoreno.padelApp.model.Tournament;

// Versión resumida para el listado: sin descripción ni pistas, que se ven en el detalle (GET /tournaments/{id})
public record TournamentListResponse(
    UUID id,
    String name,
    LocalDateTime startDate,
    LocalDateTime endDate,
    BigDecimal entryPrice,
    Integer maxSlots
) {
    public static TournamentListResponse from(Tournament tournament){
        return new TournamentListResponse(
            tournament.getPublicId(),
            tournament.getName(),
            tournament.getStartDate(),
            tournament.getEndDate(),
            tournament.getEntryPrice(),
            tournament.getMaxSlots());
    }
}
