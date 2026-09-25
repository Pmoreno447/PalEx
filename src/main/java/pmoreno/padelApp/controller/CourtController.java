package pmoreno.padelApp.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pmoreno.padelApp.dto.CourtRequest;
import pmoreno.padelApp.dto.CourtResponse;
import pmoreno.padelApp.service.CourtService;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;





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

    @PostMapping()
    public CourtResponse postCourt(@RequestBody CourtRequest courtRequest) {
        return courtService.createCourt(courtRequest);
    }

    @PatchMapping("/{courtId}")
    public CourtResponse updateCourt(@PathVariable Long courtId ,@RequestBody CourtRequest courtRequest){
        return courtService.updateCourt(courtId, courtRequest);
    }
    
    
}
