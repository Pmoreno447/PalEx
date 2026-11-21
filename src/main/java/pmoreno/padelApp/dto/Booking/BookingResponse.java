package pmoreno.padelApp.dto.Booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import pmoreno.padelApp.model.Booking;
import pmoreno.padelApp.model.BookingState;

public record BookingResponse(
    UUID id,
    Long courtId,
    LocalDateTime initTime,
    LocalDateTime endTime,
    BookingState bookingState,
    BigDecimal amount
) {
   public static BookingResponse from(Booking booking){
        return new BookingResponse(booking.getPublicId(),
                                booking.getCourt().getId(),
                                booking.getInitDateTime(), 
                                booking.getEndDateTime(), 
                                booking.getState(),
                                booking.getAmount());
   } 
}
