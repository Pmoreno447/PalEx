package pmoreno.padelApp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pmoreno.padelApp.dto.Court.AvailabilityResponse;
import pmoreno.padelApp.dto.Court.CourtCreateRequest;
import pmoreno.padelApp.dto.Court.CourtResponse;
import pmoreno.padelApp.dto.Court.CourtUpdateRequest;
import pmoreno.padelApp.exceptions.BadRequestException;
import pmoreno.padelApp.exceptions.ResourceNotFoundException;
import pmoreno.padelApp.model.BookingState;
import pmoreno.padelApp.model.Court;
import pmoreno.padelApp.repository.BookingRepository;
import pmoreno.padelApp.repository.CourtRepository;

@ExtendWith(MockitoExtension.class)
public class CourtServiceTests {
    private static final int MAX_DAYS_AHEAD = 2;

    @Mock
    private CourtRepository courtRepository;

    @Mock
    private BookingRepository bookingRepository;

    private CourtService courtService;

    private Court courtTest;

    @BeforeEach
    void setUp(){
        courtService = new CourtService(courtRepository, bookingRepository, MAX_DAYS_AHEAD);
        courtTest = new Court("Old Court", BigDecimal.valueOf(10.0), true, 90, LocalTime.of(16, 0), LocalTime.of(19, 0));
    }

    @Test
    @DisplayName("Should create a court")
    void shouldCreateCourt(){
        Court newCourt = new Court("Court Test", BigDecimal.valueOf(5.0), false, 90, LocalTime.of(16, 0), LocalTime.of(21, 0));

        CourtCreateRequest courtRequest = CourtCreateRequest.from(newCourt);

        when(courtRepository.save(any(Court.class))).thenAnswer(inv -> inv.getArgument(0));

        courtService.createCourt(courtRequest);

        ArgumentCaptor<Court> captor = ArgumentCaptor.forClass(Court.class);
        verify(courtRepository).save(captor.capture());
        Court saved = captor.getValue();

        assertEquals("Court Test", saved.getName());
        assertEquals(BigDecimal.valueOf(5.0), saved.getPrice());
        assertEquals(90, saved.getSlotMinutes());
        assertEquals(LocalTime.of(16, 0), saved.getOpenTime());
        assertEquals(LocalTime.of(21, 0), saved.getCloseTime());
    }

    @Test
    @DisplayName("Should not create a court that opens after it closes")
    void shouldNotCreateCourt_whenOpenTimeIsAfterCloseTime(){
        CourtCreateRequest request = new CourtCreateRequest("Court Test", BigDecimal.valueOf(5.0), true, 90,
                                                LocalTime.of(21, 0), LocalTime.of(16, 0));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            courtService.createCourt(request)
        );

        assertEquals("La hora de cierre no puede ser anterior a la de apertura", exception.getMessage());
        verify(courtRepository, never()).save(any());
    }

    @Test
    @DisplayName("Admin should see all courts, including inactive ones")
    void adminShouldGetAllCourts(){
        when(courtRepository.findAll()).thenReturn(List.of(courtTest));

        List<CourtResponse> courts = courtService.getCourts(true);

        assertEquals(1, courts.size());
        verify(courtRepository, never()).findByActiveTrue();
    }

    @Test
    @DisplayName("Non admin should see only active courts")
    void nonAdminShouldGetOnlyActiveCourts(){
        when(courtRepository.findByActiveTrue()).thenReturn(List.of(courtTest));

        List<CourtResponse> courts = courtService.getCourts(false);

        assertEquals(1, courts.size());
        verify(courtRepository, never()).findAll();
    }

    @Test
    @DisplayName("Should update only the fields included in the request")
    void shouldUpdateOnlyIncludedFields(){
        when(courtRepository.findById(1L)).thenReturn(Optional.of(courtTest));

        CourtUpdateRequest request = new CourtUpdateRequest("New Court", null, null, null, null, null);
        CourtResponse response = courtService.updateCourt(1L, request);

        assertEquals("New Court", response.name());

        // El resto de valores sigue igual
        assertEquals(BigDecimal.valueOf(10.0), response.price());
        assertEquals(90, response.slotMinutes());
        assertEquals(LocalTime.of(16, 0), response.openTime());
        assertEquals(LocalTime.of(19, 0), response.closeTime());
        assertEquals(true, courtTest.getActive());
    }

    @Test
    @DisplayName("Should update every field when all are included")
    void shouldUpdateAllFields(){
        when(courtRepository.findById(1L)).thenReturn(Optional.of(courtTest));

        CourtUpdateRequest request = new CourtUpdateRequest("New Court", BigDecimal.valueOf(20.0), false, 60,
                                                LocalTime.of(9, 0), LocalTime.of(23, 0));
        CourtResponse response = courtService.updateCourt(1L, request);

        assertEquals("New Court", response.name());
        assertEquals(BigDecimal.valueOf(20.0), response.price());
        assertEquals(60, response.slotMinutes());
        assertEquals(LocalTime.of(9, 0), response.openTime());
        assertEquals(LocalTime.of(23, 0), response.closeTime());
        assertEquals(false, courtTest.getActive());
    }

    @Test
    @DisplayName("Should not update a court that does not exist")
    void shouldNotUpdateMissingCourt(){
        CourtUpdateRequest request = new CourtUpdateRequest("New Court", null, null, null, null, null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            courtService.updateCourt(99L, request)
        );

        assertEquals("Pista con id 99 no encontrada.", exception.getMessage());
    }

    @Test
    @DisplayName("Should not update only the close time to before the current open time")
    void shouldNotUpdateCourt_whenNewCloseTimeIsBeforeCurrentOpenTime(){
        when(courtRepository.findById(1L)).thenReturn(Optional.of(courtTest));

        // courtTest abre a las 16:00: el request solo trae el cierre, el DTO no puede detectarlo
        CourtUpdateRequest request = new CourtUpdateRequest(null, null, null, null, null, LocalTime.of(10, 0));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            courtService.updateCourt(1L, request)
        );

        assertEquals("La hora de cierre no puede ser anterior a la de apertura", exception.getMessage());
    }

    @Test
    @DisplayName("Should not update only the open time to after the current close time")
    void shouldNotUpdateCourt_whenNewOpenTimeIsAfterCurrentCloseTime(){
        when(courtRepository.findById(1L)).thenReturn(Optional.of(courtTest));

        // courtTest cierra a las 19:00
        CourtUpdateRequest request = new CourtUpdateRequest(null, null, null, null, LocalTime.of(20, 0), null);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            courtService.updateCourt(1L, request)
        );

        assertEquals("La hora de cierre no puede ser anterior a la de apertura", exception.getMessage());
    }

    @Test
    @DisplayName("Should return the availability grid of an active court")
    void shouldReturnAvailability(){
        LocalDate day = LocalDate.now().plusDays(1);
        when(courtRepository.findById(1L)).thenReturn(Optional.of(courtTest));
        when(bookingRepository.findOccupiedStarts(any(), any(), any(), any())).thenReturn(new ArrayDeque<>());

        List<AvailabilityResponse> availability = courtService.getCourtDisponibility(1L, day, day);

        // De 16:00 a 19:00 con turnos de 90 minutos salen 2 turnos libres
        assertEquals(2, availability.size());
        assertEquals(day.atTime(16, 0), availability.get(0).start());
        assertEquals(day.atTime(19, 0), availability.get(1).end());

        // Busca las reservas en el rango de apertura a cierre y solo las que ocupan pista
        verify(bookingRepository).findOccupiedStarts(1L, day.atTime(16, 0), day.atTime(19, 0),
                                                     EnumSet.of(BookingState.PENDING, BookingState.COMPLETED));
    }

    @Test
    @DisplayName("Should accept a range ending exactly at the max days ahead")
    void shouldAcceptRangeAtMaxDaysAhead(){
        LocalDate today = LocalDate.now();
        when(courtRepository.findById(1L)).thenReturn(Optional.of(courtTest));
        when(bookingRepository.findOccupiedStarts(any(), any(), any(), any())).thenReturn(new ArrayDeque<>());

        List<AvailabilityResponse> availability =
            courtService.getCourtDisponibility(1L, today, today.plusDays(MAX_DAYS_AHEAD));

        // 3 días (hoy, mañana y pasado) x 2 turnos
        assertEquals(6, availability.size());
    }

    @Test
    @DisplayName("Should reject a range starting in the past")
    void shouldRejectFromInThePast(){
        LocalDate yesterday = LocalDate.now().minusDays(1);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            courtService.getCourtDisponibility(1L, yesterday, LocalDate.now())
        );

        assertEquals("Rango de fechas no válido", exception.getMessage());
        verifyNoInteractions(courtRepository, bookingRepository);
    }

    @Test
    @DisplayName("Should reject a range where to is before from")
    void shouldRejectToBeforeFrom(){
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            courtService.getCourtDisponibility(1L, tomorrow, LocalDate.now())
        );

        assertEquals("Rango de fechas no válido", exception.getMessage());
        verifyNoInteractions(courtRepository, bookingRepository);
    }

    @Test
    @DisplayName("Should reject a range beyond the max days ahead")
    void shouldRejectBeyondMaxDaysAhead(){
        LocalDate today = LocalDate.now();

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            courtService.getCourtDisponibility(1L, today, today.plusDays(MAX_DAYS_AHEAD + 1))
        );

        assertEquals("Rango de fechas no válido", exception.getMessage());
        verifyNoInteractions(courtRepository, bookingRepository);
    }

    @Test
    @DisplayName("Should fail when the court does not exist")
    void shouldFailWhenCourtNotFound(){
        LocalDate today = LocalDate.now();

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            courtService.getCourtDisponibility(99L, today, today)
        );

        assertEquals("Pista con id 99 no encontrada.", exception.getMessage());
        verifyNoInteractions(bookingRepository);
    }

    @Test
    @DisplayName("Should reject an inactive court")
    void shouldRejectInactiveCourt(){
        LocalDate today = LocalDate.now();
        courtTest.setActive(false);
        when(courtRepository.findById(anyLong())).thenReturn(Optional.of(courtTest));

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            courtService.getCourtDisponibility(1L, today, today)
        );

        // Mismo mensaje que si no existiera, para no revelar que la pista existe (ADR-006)
        assertEquals("Pista con id 1 no encontrada.", exception.getMessage());
        verifyNoInteractions(bookingRepository);
    }
}
