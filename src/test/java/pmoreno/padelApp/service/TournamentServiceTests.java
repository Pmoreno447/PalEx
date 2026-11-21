package pmoreno.padelApp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pmoreno.padelApp.dto.Tournament.TournamentCreateRequest;
import pmoreno.padelApp.dto.Tournament.TournamentResponse;
import pmoreno.padelApp.exceptions.BadRequestException;
import pmoreno.padelApp.exceptions.ConflictException;
import pmoreno.padelApp.exceptions.ResourceNotFoundException;
import pmoreno.padelApp.model.Court;
import pmoreno.padelApp.model.Tournament;
import pmoreno.padelApp.repository.BookingRepository;
import pmoreno.padelApp.repository.CourtRepository;
import pmoreno.padelApp.repository.TournamentRepository;

@ExtendWith(MockitoExtension.class)
public class TournamentServiceTests {
    private static final LocalDateTime START = LocalDateTime.now().plusDays(7).withHour(9).withMinute(0).withSecond(0).withNano(0);
    private static final LocalDateTime END = START.plusHours(10);

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private CourtRepository courtRepository;

    @Mock
    private BookingRepository bookingRepository;

    private TournamentService tournamentService;

    private Court courtTest;

    // Torneo válido salvo por lo que cambie cada test: las pistas y las fechas
    private TournamentCreateRequest request(Set<Long> courtIds, LocalDateTime start, LocalDateTime end){
        return new TournamentCreateRequest("Torneo Test", "**Torneo** de prueba", start, end,
                                           new BigDecimal("15.00"), 16, courtIds);
    }

    @BeforeEach
    void setUp(){
        tournamentService = new TournamentService(tournamentRepository, courtRepository, bookingRepository);
        courtTest = new Court("Court Test", BigDecimal.valueOf(12.0), true, 90, LocalTime.of(9, 0), LocalTime.of(23, 0));
    }

    @Test
    @DisplayName("Should save the tournament with its courts when the period is free")
    void shouldCreateTournament_whenCourtsAreFree(){
        when(courtRepository.findAllById(Set.of(1L))).thenReturn(List.of(courtTest));
        when(bookingRepository.existsOverlapping(eq(1L), eq(START), eq(END), any())).thenReturn(false);
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(inv -> inv.getArgument(0));

        TournamentResponse response = tournamentService.createTournament(request(Set.of(1L), START, END));

        ArgumentCaptor<Tournament> captor = ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository).save(captor.capture());
        Tournament saved = captor.getValue();

        assertEquals("Torneo Test", saved.getName());
        assertEquals(START, saved.getStartDate());
        assertEquals(END, saved.getEndDate());
        assertTrue(saved.getCourts().contains(courtTest));
        assertEquals("Court Test", response.courts().get(0).name());
    }

    @Test
    @DisplayName("Should reject the tournament when a court already has bookings in that period")
    void shouldNotCreateTournament_whenCourtHasBookings(){
        when(courtRepository.findAllById(Set.of(1L))).thenReturn(List.of(courtTest));
        when(bookingRepository.existsOverlapping(eq(1L), eq(START), eq(END), any())).thenReturn(true);

        assertThrows(ConflictException.class, () ->
            tournamentService.createTournament(request(Set.of(1L), START, END))
        );

        verify(tournamentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject the tournament when one of the courts does not exist")
    void shouldNotCreateTournament_whenCourtDoesNotExist(){
        // Se piden la 1 y la 99, pero solo existe la 1
        when(courtRepository.findAllById(Set.of(1L, 99L))).thenReturn(List.of(courtTest));

        assertThrows(ResourceNotFoundException.class, () ->
            tournamentService.createTournament(request(Set.of(1L, 99L), START, END))
        );

        verify(tournamentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject the tournament when it ends before it starts")
    void shouldNotCreateTournament_whenEndIsBeforeStart(){
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            tournamentService.createTournament(request(Set.of(1L), END, START))
        );

        assertEquals("La fecha de fin tiene que ser posterior a la de comienzo", exception.getMessage());
        // Se rechaza antes de tocar la base de datos
        verifyNoInteractions(courtRepository, bookingRepository, tournamentRepository);
    }
}
