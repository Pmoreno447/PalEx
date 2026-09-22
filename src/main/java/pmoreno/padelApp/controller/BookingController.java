package pmoreno.padelApp.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;


@RestController 
@RequestMapping("/bookings")
public class BookingController {

    @GetMapping
    public String getReservations() {
        return new String("Zero turns available.");
    }

}
