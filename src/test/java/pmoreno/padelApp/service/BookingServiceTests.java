package pmoreno.padelApp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pmoreno.padelApp.dto.Booking.BookingCreateRequest;
import pmoreno.padelApp.dto.Booking.BookingResponse;
import pmoreno.padelApp.exceptions.BadRequestException;
import pmoreno.padelApp.exceptions.ConflictException;
import pmoreno.padelApp.exceptions.ResourceNotFoundException;
import pmoreno.padelApp.model.Booking;
import pmoreno.padelApp.model.BookingState;
import pmoreno.padelApp.model.Court;
import pmoreno.padelApp.model.Role;
import pmoreno.padelApp.model.User;
import pmoreno.padelApp.repository.BookingRepository;
import pmoreno.padelApp.repository.CourtRepository;
import pmoreno.padelApp.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTests {
    private static final int MAX_DAYS_AHEAD = 4;
    private static final int MAX_PENDING = 2;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CourtRepository courtRepository;

    @Mock
    private UserRepository userRepository;

    private BookingService bookingService;

    private User userTest;

    private Court courtTest;

    // Mañana, para quedar siempre dentro de la ventana de reserva
    private LocalDateTime tomorrowAt(int hour, int minute){
        return LocalDateTime.now().plusDays(1).toLocalDate().atTime(hour, minute);
    }

    @BeforeEach
    void setUp(){
        bookingService = new BookingService(bookingRepository, courtRepository, userRepository, MAX_DAYS_AHEAD, MAX_PENDING);
        userTest = new User(1L, "Test Name", "user-1", "test@example.com", "CoolTestUser", "+34123456789", Role.USER);
        // Abre a las 09:00 con turnos de 90 min: 09:00, 10:30, 12:00, 13:30...
        courtTest = new Court("Court Test", BigDecimal.valueOf(12.0), true, 90, LocalTime.of(9, 0), LocalTime.of(23, 0));
    }

    @Test
    @DisplayName("Should save a pending booking on a free grid slot, dropping the seconds")
    void shouldCreateBooking_whenSlotIsOnGridAndFree(){
        when(userRepository.findByProviderId("user-1")).thenReturn(Optional.of(userTest));
        when(courtRepository.findById(1L)).thenReturn(Optional.of(courtTest));
        when(bookingRepository.existsOverlapping(any(), any(), any(), any())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        bookingService.createNewBooking("user-1", new BookingCreateRequest(1L, tomorrowAt(13, 30).withSecond(30)));

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        Booking saved = captor.getValue();

        assertEquals(tomorrowAt(13, 30), saved.getInitDateTime());
        assertEquals(tomorrowAt(15, 0), saved.getEndDateTime());
        assertEquals(BookingState.PENDING, saved.getState());
        assertEquals(BigDecimal.valueOf(12.0), saved.getAmount());
        assertEquals(userTest, saved.getUser());
    }

    @Test
    @DisplayName("Should reject a start time that is not on the court grid")
    void shouldNotCreateBooking_whenSlotIsOffGrid(){
        when(userRepository.findByProviderId("user-1")).thenReturn(Optional.of(userTest));
        when(courtRepository.findById(1L)).thenReturn(Optional.of(courtTest));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            bookingService.createNewBooking("user-1", new BookingCreateRequest(1L, tomorrowAt(13, 45)))
        );

        assertEquals("El turno no coincide con el horario de la pista", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject a new booking when the user already has the max pending bookings")
    void shouldNotCreateBooking_whenUserHasMaxPending(){
        when(userRepository.findByProviderId("user-1")).thenReturn(Optional.of(userTest));
        when(bookingRepository.countByUserAndState(userTest, BookingState.PENDING)).thenReturn((long) MAX_PENDING);

        ConflictException exception = assertThrows(ConflictException.class, () ->
            bookingService.createNewBooking("user-1", new BookingCreateRequest(1L, tomorrowAt(13, 30)))
        );

        assertEquals("No puedes tener más de " + MAX_PENDING + " reservas pendientes de pago", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return the booking when it belongs to the user")
    void shouldGetBooking_whenUserIsOwner(){
        Booking booking = new Booking(tomorrowAt(10, 30), tomorrowAt(12, 0), BigDecimal.valueOf(12.0),
                                      BookingState.PENDING, courtTest, userTest);
        when(bookingRepository.findByPublicIdAndUserProviderId(booking.getPublicId(), "user-1"))
            .thenReturn(Optional.of(booking));

        BookingResponse response = bookingService.getBooking("user-1", booking.getPublicId());

        assertEquals(booking.getPublicId(), response.id());
        assertEquals(tomorrowAt(10, 30), response.initTime());
    }

    @Test
    @DisplayName("Should not find a booking that does not exist or belongs to another user")
    void shouldNotGetBooking_whenNotFoundForUser(){
        UUID bookingId = UUID.randomUUID();

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            bookingService.getBooking("user-1", bookingId)
        );

        assertEquals("Reserva con id " + bookingId + " no encontrada.", exception.getMessage());
    }
}
