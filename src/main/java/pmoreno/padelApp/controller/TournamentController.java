package pmoreno.padelApp.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pmoreno.padelApp.dto.Tournament.TournamentCreateRequest;
import pmoreno.padelApp.dto.Tournament.TournamentResponse;
import pmoreno.padelApp.service.TournamentService;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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
}
