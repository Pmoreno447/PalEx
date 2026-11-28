package pmoreno.padelApp.dto.Tournament;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import pmoreno.padelApp.dto.Court.CourtResponse;
import pmoreno.padelApp.model.Tournament;

public record TournamentResponse(
    UUID id,
    String name,
    String description,
    LocalDateTime startDate,
    LocalDateTime endDate,
    BigDecimal entryPrice,
    Integer maxSlots,
    Integer actualSlots,
    List<CourtResponse> courts
) {
    // Las pistas se cargan en diferido (LAZY): hay que llamarlo con la sesión de Hibernate abierta, dentro del service
    public static TournamentResponse from(Tournament tournament){
        return new TournamentResponse(
            tournament.getPublicId(),
            tournament.getName(),
            tournament.getDescription(),
            tournament.getStartDate(),
            tournament.getEndDate(),
            tournament.getEntryPrice(),
            tournament.getMaxSlots(),
            tournament.getActualSlots(),
            tournament.getCourts().stream()
                .map(CourtResponse::from)
                .toList());
    }
}
