package pmoreno.padelApp.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pmoreno.padelApp.dto.AvailabilityResponse;
import pmoreno.padelApp.dto.CourtRequest;
import pmoreno.padelApp.dto.CourtResponse;
import pmoreno.padelApp.model.BookingState;
import pmoreno.padelApp.model.Court;
import pmoreno.padelApp.repository.BookingRepository;
import pmoreno.padelApp.repository.CourtRepository;
import pmoreno.padelApp.repository.projection.OccupiedSlot;
import pmoreno.padelApp.service.domain.AvailabilityCalculator;

@Service 
public class CourtService {
    private final CourtRepository courtRepository;
    private final BookingRepository bookingRepository;
    private final int maxDaysAhead;

    public CourtService(CourtRepository courtRepository, BookingRepository bookingRepository, @Value("${app.booking.max-days-ahead}") int maxDaysAhead){
        this.courtRepository = courtRepository;
        this.bookingRepository = bookingRepository;
        this.maxDaysAhead = maxDaysAhead;
    }

    @Transactional(readOnly = true)
    public List<CourtResponse> getCourts(boolean isAdmin){
        List<Court> courts = isAdmin
            ? courtRepository.findAll()
            : courtRepository.findByActiveTrue();

        return courts.stream()
            .map(CourtResponse::from)
            .toList();
    }

    @Transactional 
    public CourtResponse createCourt(CourtRequest courtRequest){
        Court c =  courtRepository.save(courtRequest.toModel());

        return CourtResponse.from(c);
    }

    @Transactional 
    public CourtResponse updateCourt(Long courtId, CourtRequest courtRequest){
        Court c = courtRepository.findById(courtId)
            .orElseThrow(() -> new IllegalStateException("[updateCourt]: Pista no encontrada"));

        if(courtRequest.name() != null){
            c.setName(courtRequest.name());
        }
        if(courtRequest.price() != null){
            c.setPrice(courtRequest.price());
        }
        if(courtRequest.active() != null){
            c.setActive(courtRequest.active());
        }
        if(courtRequest.slotMinutes() != null){
            c.setSlotMinutes(courtRequest.slotMinutes());
        }
        if(courtRequest.openTime() != null){
            c.setOpenTime(courtRequest.openTime());
        }
        if(courtRequest.closTime() != null){
            c.setCloseTime(courtRequest.closTime());
        }

        return CourtResponse.from(c);


    }

    @Transactional (readOnly = true)
    public List<AvailabilityResponse> getCourtDisponibility(Long courtId, LocalDate from, LocalDate to){
        if(from.isBefore(LocalDate.now()) 
            || to.isBefore(from) 
            || to.isAfter(LocalDate.now().plusDays(maxDaysAhead))){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rango de fechas no válido");
        }

        Court c = courtRepository.findById(courtId)
            .orElseThrow(() -> new IllegalStateException("[selectCourt]: Pista no encontrada"));

        if(!c.getActive()){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La pista está dada de baja");
        }

        LocalDateTime start = from.atTime(c.getOpenTime());
        LocalDateTime end = to.atTime(c.getCloseTime());

        ArrayDeque<OccupiedSlot> bookings = 
        bookingRepository.findOccupiedStarts(courtId, start, end, EnumSet.of(BookingState.PENDING, BookingState.COMPLETED));
        
        return AvailabilityCalculator.getAvailability(c.getOpenTime(), c.getCloseTime(), start, end, c.getSlotMinutes(), bookings);     
    }
}
