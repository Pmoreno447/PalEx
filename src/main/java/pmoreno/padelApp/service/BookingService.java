package pmoreno.padelApp.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import pmoreno.padelApp.dto.Booking.BookingCreateRequest;
import pmoreno.padelApp.dto.Booking.BookingResponse;
import pmoreno.padelApp.exceptions.BadRequestException;
import pmoreno.padelApp.exceptions.ConflictException;
import pmoreno.padelApp.exceptions.ResourceNotFoundException;
import pmoreno.padelApp.model.Booking;
import pmoreno.padelApp.model.BookingState;
import pmoreno.padelApp.model.Court;
import pmoreno.padelApp.model.User;
import pmoreno.padelApp.repository.BookingRepository;
import pmoreno.padelApp.repository.CourtRepository;
import pmoreno.padelApp.repository.UserRepository;

@Service 
public class BookingService {
    private final BookingRepository bookingRepository;
    private final CourtRepository courtRepository;
    private final UserRepository userRepository;
    private final int maxDaysAhead;
    private final int maxPendingBookings;

    public BookingService(BookingRepository bookingRepository, CourtRepository courtRepository, UserRepository userRepository,
                          @Value("${app.booking.max-days-ahead}") int maxDaysAhead,
                          @Value("${app.booking.max-pending}") int maxPendingBookings){
        this.bookingRepository = bookingRepository;
        this.courtRepository = courtRepository;
        this.userRepository = userRepository;
        this.maxDaysAhead = maxDaysAhead;
        this.maxPendingBookings = maxPendingBookings;
    }

    // La pista ya llega comprobada (existe y está activa): aquí se mira la fecha y si el turno está libre
    private boolean isAvailable(Court court, LocalDateTime start){
        // Misma ventana que la consulta de disponibilidad (ADR-004): ni turnos ya empezados ni más allá de maxDaysAhead.
        // Es un 400 y no un 409: la fecha es inválida en sí misma, no choca con otra reserva
        if(start.isBefore(LocalDateTime.now())
            || start.toLocalDate().isAfter(LocalDate.now().plusDays(maxDaysAhead))){
            throw new BadRequestException("Fecha de reserva no válida");
        }

        // El turno tiene que ser uno de la rejilla: empezar en apertura + n * slotMinutes y terminar antes del cierre.
        // Se mide como minutos desde la apertura para no depender de LocalTime, que da la vuelta a medianoche (22:30 + 90 = 00:00)
        long fromOpen = Duration.between(court.getOpenTime(), start.toLocalTime()).toMinutes();
        long openMinutes = Duration.between(court.getOpenTime(), court.getCloseTime()).toMinutes();

        if(fromOpen < 0 || fromOpen + court.getSlotMinutes() > openMinutes || fromOpen % court.getSlotMinutes() != 0){
            throw new BadRequestException("El turno no coincide con el horario de la pista");
        }

        LocalDateTime end = start.plusMinutes(court.getSlotMinutes());

        return !bookingRepository.existsOverlapping(court.getId(), start, end,
                                                   EnumSet.of(BookingState.PENDING, BookingState.COMPLETED));
    }

    public BookingResponse createNewBooking(String providerId, BookingCreateRequest bookingRequest){
        // Las reservas empiezan en minuto exacto: se descartan segundos para que la rejilla y la restricción única cuadren
        LocalDateTime start = bookingRequest.initTime().truncatedTo(ChronoUnit.MINUTES);

        // El usuario sale del token, nunca del body, para que nadie pueda reservar en nombre de otro
        User user = userRepository.findByProviderId(providerId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario " + providerId +  " no sincronizado con la base de datos."));

        // Límite contra el acaparamiento de turnos: reservas sin pagar que un usuario puede tener a la vez
        if(bookingRepository.countByUserAndState(user, BookingState.PENDING) >= maxPendingBookings){
            throw new ConflictException("No puedes tener más de " + maxPendingBookings + " reservas pendientes de pago");
        }

        Court court = courtRepository.findById(bookingRequest.courtId())
            .orElseThrow(() -> new ResourceNotFoundException("Pista " + bookingRequest.courtId() + " no encontrada"));

        // Una pista inactiva no se puede reservar y responde igual que si no existiera, para no revelarla (ADR-006)
        if(!court.getActive()){
            throw new ResourceNotFoundException("Pista " + bookingRequest.courtId() + " no encontrada");
        }

        // Fecha, rejilla y solape se validan en el servidor: la disponibilidad que vio el cliente puede estar desfasada o ser inventada
        if(!isAvailable(court, start)){
            throw new ConflictException("El turno seleccionado para la fecha " + start + " no está disponible");
        }

        // Nace pendiente de pago y guarda el precio actual de la pista, por si este cambia después
        Booking booking = new Booking(start,
                                        start.plusMinutes(court.getSlotMinutes()),
                                        court.getPrice(), 
                                        BookingState.PENDING, 
                                        court, 
                                        user);
                                        
        
        return BookingResponse.from(bookingRepository.save(booking)); 
    }

    public BookingResponse getBooking(String providerId, UUID bookingId){
        return BookingResponse.from(bookingRepository.findByPublicIdAndUserProviderId(bookingId, providerId)
            .orElseThrow(()-> new ResourceNotFoundException("Reserva con id " + bookingId + " no encontrada.")));
    }
}
