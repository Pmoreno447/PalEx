package pmoreno.padelApp.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pmoreno.padelApp.dto.CourtResponse;
import pmoreno.padelApp.service.CourtService;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;




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
    
}
