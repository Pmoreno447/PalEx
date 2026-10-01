package pmoreno.padelApp.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pmoreno.padelApp.dto.Court.AvailabilityResponse;
import pmoreno.padelApp.dto.Court.CourtCreateRequest;
import pmoreno.padelApp.dto.Court.CourtResponse;
import pmoreno.padelApp.dto.Court.CourtUpdateRequest;
import pmoreno.padelApp.service.CourtService;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;






@RestController
@RequestMapping("/courts")
public class CourtController {
    private final CourtService courtService;

    public CourtController(CourtService courtService){
        this.courtService = courtService;
    }

    @GetMapping
    public List<CourtResponse> getCourts(Authentication authentication) {
        if(authentication != null){
            boolean isAdmin = authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            
            return courtService.getCourts(isAdmin);
        }

        return courtService.getCourts(false);
    }

    @Valid 
    @PostMapping()
    public CourtResponse postCourt(@RequestBody CourtCreateRequest courtRequest) {
        return courtService.createCourt(courtRequest);
    }

    @Valid 
    @PatchMapping("/{courtId}")
    public CourtResponse updateCourt(@PathVariable Long courtId ,@RequestBody CourtUpdateRequest courtRequest){
        return courtService.updateCourt(courtId, courtRequest);
    }

    @GetMapping("/{courtId}/availability")
    public List<AvailabilityResponse> getCourtAvailability(@PathVariable Long courtId, @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return courtService.getCourtDisponibility(courtId, from, to);
    }
    
    
    
}
