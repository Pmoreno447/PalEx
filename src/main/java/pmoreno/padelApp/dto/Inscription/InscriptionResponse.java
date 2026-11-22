package pmoreno.padelApp.dto.Inscription;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import pmoreno.padelApp.model.BookingState;
import pmoreno.padelApp.model.TournamentRegistration;

// Inscripción tal y como la ve el usuario en su panel. Del torneo solo va lo justo: con su id se consulta el resto
public record InscriptionResponse(
    UUID id,
    BigDecimal amount,
    BookingState state,
    LocalDateTime registeredAt,
    UUID tournamentId,
    String tournamentName
) {
    // El torneo se carga en diferido (LAZY): hay que llamarlo con la sesión de Hibernate abierta, dentro del service
    public static InscriptionResponse from(TournamentRegistration registration){
        return new InscriptionResponse(
            registration.getPublicId(),
            registration.getAmount(),
            registration.getState(),
            registration.getRegisteredAt(),
            registration.getTournament().getPublicId(),
            registration.getTournament().getName());
    }
}
