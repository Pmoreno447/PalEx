package pmoreno.padelApp.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import pmoreno.padelApp.dto.Booking.BookingCreateRequest;
import pmoreno.padelApp.dto.Booking.BookingResponse;
import pmoreno.padelApp.service.BookingService;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;



@RestController 
@RequestMapping("/bookings")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService){
        this.bookingService = bookingService;
    }

    @PostMapping()
    public ResponseEntity<BookingResponse> postBooking(Authentication authentication, @Valid @RequestBody BookingCreateRequest bookingRequest) {
        BookingResponse bookingResponse = bookingService.createNewBooking(authentication.getName(), bookingRequest);
        
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{bookingId}")
            .buildAndExpand(bookingResponse.id())
            .toUri();

        return ResponseEntity.created(location).body(bookingResponse);
    }

    @GetMapping("/{bookingId}")
    public BookingResponse getBooking(@PathVariable UUID bookingId, Authentication authentication) {
        return bookingService.getBooking(authentication.getName(), bookingId);
    }
    
    
    public String getReservations() {
        return new String("Zero turns available.");
    }

}
