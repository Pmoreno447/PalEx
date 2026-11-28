package pmoreno.padelApp.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pmoreno.padelApp.dto.Tournament.TournamentCreateRequest;
import pmoreno.padelApp.dto.Tournament.TournamentListResponse;
import pmoreno.padelApp.dto.Tournament.TournamentResponse;
import pmoreno.padelApp.service.TournamentService;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
@RequestMapping("/tournaments")
public class TournamentController {
    private final TournamentService tournamentService;

    public TournamentController(TournamentService tournamentService){
        this.tournamentService = tournamentService;
    }

    @PostMapping()
    public ResponseEntity<TournamentResponse> postTournament(@Valid @RequestBody TournamentCreateRequest tournamentRequest) {
        TournamentResponse created = tournamentService.createTournament(tournamentRequest);

        // Location: /tournaments/{id} construido a partir de la URL de esta petición
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id())
            .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @GetMapping()
    public List<TournamentListResponse> getTournaments(Authentication authentication) {
        return tournamentService.getTournaments(isAdmin(authentication));
    }

    @GetMapping("/{id}")
    public TournamentResponse getTournament(@PathVariable("id") UUID publicId, Authentication authentication) {
        return tournamentService.getTournament(publicId, isAdmin(authentication));
    }

    // Los GET son públicos: sin token, authentication llega null y se trata como un usuario normal
    private boolean isAdmin(Authentication authentication){
        return authentication != null
            && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
