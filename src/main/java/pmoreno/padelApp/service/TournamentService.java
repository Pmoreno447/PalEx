package pmoreno.padelApp.service;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pmoreno.padelApp.dto.Tournament.TournamentCreateRequest;
import pmoreno.padelApp.dto.Tournament.TournamentListResponse;
import pmoreno.padelApp.dto.Tournament.TournamentResponse;
import pmoreno.padelApp.exceptions.BadRequestException;
import pmoreno.padelApp.exceptions.ConflictException;
import pmoreno.padelApp.exceptions.ResourceNotFoundException;
import pmoreno.padelApp.model.BookingState;
import pmoreno.padelApp.model.Court;
import pmoreno.padelApp.model.Tournament;
import pmoreno.padelApp.model.TournamentState;
import pmoreno.padelApp.repository.BookingRepository;
import pmoreno.padelApp.repository.CourtRepository;
import pmoreno.padelApp.repository.TournamentRepository;

@Service 
public class TournamentService {
    private final TournamentRepository tournamentRepository;
    private final CourtRepository courtRepository;
    private final BookingRepository bookingRepository;

    public TournamentService(TournamentRepository tournamentRepository, 
                                CourtRepository courtRepository,
                                BookingRepository bookingRepository){
        this.tournamentRepository = tournamentRepository;
        this.courtRepository = courtRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional 
    public TournamentResponse createTournament(TournamentCreateRequest tournamentCreateRequest){
        // Comprueba si la fecha de fin es posterior a la de inicio
        if (!tournamentCreateRequest.endDate().isAfter(tournamentCreateRequest.startDate())) {
            throw new BadRequestException("La fecha de fin tiene que ser posterior a la de comienzo");
        }

        // Comprueba si las pistas existen
        List<Court> courts = courtRepository.findAllById(tournamentCreateRequest.courtIds());
        if (courts.size() != tournamentCreateRequest.courtIds().size()) {
            throw new ResourceNotFoundException("Alguna de las pistas indicadas no existe");
        }

        // Comprobamos si esta disponible el espacio de tiempo seleccionado:
        for(Long courtId :tournamentCreateRequest.courtIds()){
            boolean existBooking = bookingRepository.existsOverlapping(courtId, tournamentCreateRequest.startDate(), tournamentCreateRequest.endDate(), EnumSet.of(BookingState.PENDING, BookingState.COMPLETED));

            if(existBooking){
                throw new ConflictException("Error al crear torneo. El plazo ya contiene reservas");
            }
        }

        Tournament t = new Tournament(tournamentCreateRequest.name(), 
                                        tournamentCreateRequest.description(),
                                        tournamentCreateRequest.startDate(), 
                                        tournamentCreateRequest.endDate(), 
                                        tournamentCreateRequest.entryPrice(),
                                        tournamentCreateRequest.maxSlots(), 
                                        new HashSet<>(courts));
                                        
        Tournament saved = tournamentRepository.save(t);
        return TournamentResponse.from(saved);
    }

    @Transactional (readOnly = true)
    public TournamentResponse getTournament(UUID publicId, boolean isAdmin){
        // El admin ve cualquier torneo; el resto solo los abiertos. Si no lo puede ver, 404 como si no existiera (ADR-006)
        Tournament tournament = (isAdmin
                ? tournamentRepository.findByPublicId(publicId)
                : tournamentRepository.findByPublicIdAndState(publicId, TournamentState.OPEN))
            .orElseThrow(() -> new ResourceNotFoundException("Torneo no encontrado"));
        return TournamentResponse.from(tournament);
    }

    // Misma regla que al consultar uno: el admin ve todos, el resto solo los abiertos
    @Transactional (readOnly = true)
    public List<TournamentListResponse> getTournaments(boolean isAdmin){
        List<Tournament> tournaments = isAdmin
            ? tournamentRepository.findAllByOrderByStartDateAsc()
            : tournamentRepository.findByStateOrderByStartDateAsc(TournamentState.OPEN);

        return tournaments.stream()
            .map(TournamentListResponse::from)
            .toList();
    }
}
