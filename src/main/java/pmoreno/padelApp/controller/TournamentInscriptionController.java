package pmoreno.padelApp.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import pmoreno.padelApp.dto.Inscription.InscriptionResponse;
import pmoreno.padelApp.dto.Inscription.TournamentInscriptionRequest;
import pmoreno.padelApp.dto.Inscription.TournamentInscriptionResponse;
import pmoreno.padelApp.service.TournamentInscriptionService;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    // Las inscripciones del usuario del token, para su panel
    @GetMapping()
    public List<InscriptionResponse> getMyInscriptions(Authentication authentication) {
        return tournamentInscriptionService.getMyInscriptions(authentication.getName());
    }

    @GetMapping("/{inscriptionId}")
    public InscriptionResponse getInscription(@PathVariable UUID inscriptionId, Authentication authentication) {
        return tournamentInscriptionService.getInscription(authentication.getName(), inscriptionId);
    }

    // POST y no DELETE: la inscripción no se borra, pasa a cancelada y se reembolsa.
    // 204 = cancelada. Si no se puede, el service lanza la excepción y responde el handler (404, 409 o 502)
    @PostMapping("/{inscriptionId}/cancel")
    public ResponseEntity<Void> cancelInscription(@PathVariable UUID inscriptionId, Authentication authentication) {
        tournamentInscriptionService.tournamentInscriptionCancel(authentication.getName(), inscriptionId);
        return ResponseEntity.noContent().build();
    }

}
