package pmoreno.padelApp.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import pmoreno.padelApp.dto.Inscription.TournamentInscriptionRequest;
import pmoreno.padelApp.dto.Inscription.TournamentInscriptionResponse;
import pmoreno.padelApp.service.TournamentInscriptionService;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/inscriptions")
public class TournamentInscriptionController {
    private final TournamentInscriptionService tournamentInscriptionService;

    public TournamentInscriptionController(TournamentInscriptionService tournamentInscriptionService){
        this.tournamentInscriptionService = tournamentInscriptionService;
    }

    @PostMapping()
    public ResponseEntity<TournamentInscriptionResponse> postInscription(Authentication authentication, @Valid @RequestBody TournamentInscriptionRequest inscriptionRequest) {
        TournamentInscriptionResponse inscriptionResponse =
            tournamentInscriptionService.tournamentRegistration(authentication.getName(), inscriptionRequest.tournamentId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{inscriptionId}")
            .buildAndExpand(inscriptionResponse.id())
            .toUri();

        return ResponseEntity.created(location).body(inscriptionResponse);
    }
}
