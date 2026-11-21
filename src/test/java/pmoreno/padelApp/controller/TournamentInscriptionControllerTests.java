package pmoreno.padelApp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.h2console.autoconfigure.H2ConsoleProperties;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import pmoreno.padelApp.config.SecurityConfig;
import pmoreno.padelApp.config.UserJwtConverter;
import pmoreno.padelApp.dto.Inscription.TournamentInscriptionResponse;
import pmoreno.padelApp.exceptions.ResourceNotFoundException;
import pmoreno.padelApp.service.TournamentInscriptionService;

@Import (SecurityConfig.class)
@EnableConfigurationProperties (H2ConsoleProperties.class)
@WebMvcTest({TournamentInscriptionController.class})
@AutoConfigureRestTestClient
public class TournamentInscriptionControllerTests {
    private static final UUID TOURNAMENT_ID = UUID.randomUUID();
    private static final String INSCRIPTION_JSON = """
        {"tournamentId": "%s"}
        """.formatted(TOURNAMENT_ID);

    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private TournamentInscriptionService tournamentInscriptionService;

    @MockitoBean
    private UserJwtConverter userJwtConverter;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private Jwt userJwt;

    @BeforeEach
    void setUp(){
        // "Bearer tokenUser" -> usuario con ROLE_USER: inscribirse no exige ser admin
        userJwt = Jwt.withTokenValue("tokenUser").header("alg", "none").subject("user-1").build();
        when(jwtDecoder.decode("tokenUser")).thenReturn(userJwt);
        when(userJwtConverter.convert(userJwt))
            .thenReturn(new JwtAuthenticationToken(userJwt, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private void postAsUser(String json, int expectedStatus){
        restTestClient.post()
            .uri("/inscriptions")
            .header("Authorization", "Bearer tokenUser")
            .contentType(MediaType.APPLICATION_JSON)
            .body(json)
            .exchange()
            .expectStatus()
            .isEqualTo(expectedStatus);
    }

    // ---------- POST /inscriptions ----------

    @Test
    @DisplayName("Should create an inscription for the logged user")
    void shouldCreateInscription_whenUserIsLogged(){
        UUID inscriptionId = UUID.randomUUID();
        when(tournamentInscriptionService.tournamentRegistration("user-1", TOURNAMENT_ID))
            .thenReturn(new TournamentInscriptionResponse(inscriptionId, "https://checkout.test/session"));

        restTestClient.post()
            .uri("/inscriptions")
            .header("Authorization", "Bearer tokenUser")
            .contentType(MediaType.APPLICATION_JSON)
            .body(INSCRIPTION_JSON)
            .exchange()
            .expectStatus()
            .isCreated()
            .expectHeader()
                .valueMatches("Location", ".*/inscriptions/" + inscriptionId)
            .expectBody()
                .jsonPath("$.checkoutUrl").isEqualTo("https://checkout.test/session");

        // El usuario se toma del token (sub), no del body
        verify(tournamentInscriptionService).tournamentRegistration(eq("user-1"), eq(TOURNAMENT_ID));
    }

    @Test
    @DisplayName("Should return unauthorized when there is no token")
    void shouldGetUnauthorized_whenNoToken(){
        restTestClient.post()
            .uri("/inscriptions")
            .contentType(MediaType.APPLICATION_JSON)
            .body(INSCRIPTION_JSON)
            .exchange()
            .expectStatus()
            .isUnauthorized();

        verify(tournamentInscriptionService, never()).tournamentRegistration(any(), any());
    }

    @Test
    @DisplayName("Should return bad request when the tournament id is missing")
    void shouldGetBadRequest_whenTournamentIdIsMissing(){
        postAsUser("{}", 400);

        verify(tournamentInscriptionService, never()).tournamentRegistration(any(), any());
    }

    @Test
    @DisplayName("Should return not found when the tournament does not exist")
    void shouldGetNotFound_whenTournamentDoesNotExist(){
        when(tournamentInscriptionService.tournamentRegistration(any(), any()))
            .thenThrow(new ResourceNotFoundException("El torneo con id " + TOURNAMENT_ID + " no existe"));

        postAsUser(INSCRIPTION_JSON, 404);
    }
}
