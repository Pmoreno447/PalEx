package pmoreno.padelApp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
import pmoreno.padelApp.dto.Court.CourtResponse;
import pmoreno.padelApp.dto.Tournament.TournamentListResponse;
import pmoreno.padelApp.dto.Tournament.TournamentResponse;
import pmoreno.padelApp.exceptions.ConflictException;
import pmoreno.padelApp.exceptions.ResourceNotFoundException;
import pmoreno.padelApp.service.TournamentService;

@Import (SecurityConfig.class)
@EnableConfigurationProperties (H2ConsoleProperties.class)
@WebMvcTest({TournamentController.class})
@AutoConfigureRestTestClient
public class TournamentControllerTests {
    // Dentro de una semana, para que siempre pase el @Future
    private static final LocalDateTime START = LocalDateTime.now().plusDays(7).withHour(9).withMinute(0).withSecond(0).withNano(0);
    private static final LocalDateTime END = START.plusHours(10);

    private static final String TOURNAMENT_JSON = """
        {"name": "Torneo Nuevo", "description": "**Torneo** de prueba", "startDate": "%s", "endDate": "%s",
         "entryPrice": 15.0, "maxSlots": 16, "courtIds": [1]}
        """.formatted(START, END);

    // Sin pistas: lo rechaza el @Valid antes de llegar al service
    private static final String NO_COURTS_JSON = """
        {"name": "Torneo Nuevo", "description": "**Torneo** de prueba", "startDate": "%s", "endDate": "%s",
         "entryPrice": 15.0, "maxSlots": 16, "courtIds": []}
        """.formatted(START, END);

    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private TournamentService tournamentService;

    @MockitoBean
    private UserJwtConverter userJwtConverter;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private Jwt userJwt;
    private Jwt adminJwt;

    @BeforeEach
    void setUp(){
        // "Bearer tokenUser" -> usuario con ROLE_USER
        userJwt = Jwt.withTokenValue("tokenUser").header("alg", "none").subject("user-1").build();
        when(jwtDecoder.decode("tokenUser")).thenReturn(userJwt);
        when(userJwtConverter.convert(userJwt))
            .thenReturn(new JwtAuthenticationToken(userJwt, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        // "Bearer tokenAdmin" -> usuario con ROLE_ADMIN
        adminJwt = Jwt.withTokenValue("tokenAdmin").header("alg", "none").subject("user-2").build();
        when(jwtDecoder.decode("tokenAdmin")).thenReturn(adminJwt);
        when(userJwtConverter.convert(adminJwt))
            .thenReturn(new JwtAuthenticationToken(adminJwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private void postAsAdmin(String json, int expectedStatus){
        restTestClient.post()
            .uri("/tournaments")
            .header("Authorization", "Bearer tokenAdmin")
            .contentType(MediaType.APPLICATION_JSON)
            .body(json)
            .exchange()
            .expectStatus()
            .isEqualTo(expectedStatus);
    }

    // ---------- POST /tournaments ----------

    @Test
    @DisplayName ("Should create a tournament when the user is admin")
    void shouldCreateTournament_whenUserIsAdmin(){
        CourtResponse court = new CourtResponse(1L, "Pista 1", BigDecimal.TEN, 90, LocalTime.of(9, 0), LocalTime.of(23, 0));
        UUID tournamentId = UUID.randomUUID();
        when(tournamentService.createTournament(any()))
            .thenReturn(new TournamentResponse(tournamentId, "Torneo Nuevo", "**Torneo** de prueba", START, END,
                                               new BigDecimal("15.0"), 16, List.of(court)));

        restTestClient.post()
            .uri("/tournaments")
            .header("Authorization", "Bearer tokenAdmin")
            .contentType(MediaType.APPLICATION_JSON)
            .body(TOURNAMENT_JSON)
            .exchange()
            .expectStatus()
            .isCreated()
            .expectHeader()
                .valueMatches("Location", ".*/tournaments/" + tournamentId)
            .expectBody()
                .jsonPath("$.name").isEqualTo("Torneo Nuevo")
                .jsonPath("$.courts[0].name").isEqualTo("Pista 1");
    }

    @Test
    @DisplayName ("Should forbid creating a tournament when the user is not admin")
    void shouldForbidCreateTournament_whenUserIsNotAdmin(){
        restTestClient.post()
            .uri("/tournaments")
            .header("Authorization", "Bearer tokenUser")
            .contentType(MediaType.APPLICATION_JSON)
            .body(TOURNAMENT_JSON)
            .exchange()
            .expectStatus()
            .isForbidden();

        verify(tournamentService, never()).createTournament(any());
    }

    @Test
    @DisplayName ("Should return bad request when the tournament has no courts")
    void shouldGetBadRequest_whenTournamentHasNoCourts(){
        postAsAdmin(NO_COURTS_JSON, 400);

        verify(tournamentService, never()).createTournament(any());
    }

    @Test
    @DisplayName ("Should return not found when a court does not exist")
    void shouldGetNotFound_whenCourtDoesNotExist(){
        when(tournamentService.createTournament(any()))
            .thenThrow(new ResourceNotFoundException("Alguna de las pistas indicadas no existe"));

        postAsAdmin(TOURNAMENT_JSON, 404);
    }

    @Test
    @DisplayName ("Should return conflict when the courts already have bookings in that period")
    void shouldGetConflict_whenCourtsHaveBookings(){
        when(tournamentService.createTournament(any()))
            .thenThrow(new ConflictException("Error al crear torneo. El plazo ya contiene reservas"));

        postAsAdmin(TOURNAMENT_JSON, 409);
    }

    // ---------- GET /tournaments/{id} ----------

    private TournamentResponse tournamentResponse(UUID id){
        CourtResponse court = new CourtResponse(1L, "Pista 1", BigDecimal.TEN, 90, LocalTime.of(9, 0), LocalTime.of(23, 0));
        return new TournamentResponse(id, "Torneo Nuevo", "**Torneo** de prueba", START, END,
                                      new BigDecimal("15.0"), 16, List.of(court));
    }

    @Test
    @DisplayName ("Should return a tournament without a token, as a non admin")
    void shouldGetTournament_whenAnonymous(){
        UUID id = UUID.randomUUID();
        when(tournamentService.getTournament(id, false)).thenReturn(tournamentResponse(id));

        restTestClient.get()
            .uri("/tournaments/{id}", id)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
                .jsonPath("$.id").isEqualTo(id.toString())
                .jsonPath("$.name").isEqualTo("Torneo Nuevo");

        verify(tournamentService).getTournament(id, false);
    }

    @Test
    @DisplayName ("Should ask for the tournament as admin when the user is admin")
    void shouldGetTournamentAsAdmin_whenUserIsAdmin(){
        UUID id = UUID.randomUUID();
        when(tournamentService.getTournament(id, true)).thenReturn(tournamentResponse(id));

        restTestClient.get()
            .uri("/tournaments/{id}", id)
            .header("Authorization", "Bearer tokenAdmin")
            .exchange()
            .expectStatus()
            .isOk();

        verify(tournamentService).getTournament(id, true);
    }

    @Test
    @DisplayName ("Should return not found when the tournament cannot be seen")
    void shouldGetNotFound_whenTournamentCannotBeSeen(){
        UUID id = UUID.randomUUID();
        when(tournamentService.getTournament(id, false)).thenThrow(new ResourceNotFoundException("Torneo no encontrado"));

        restTestClient.get()
            .uri("/tournaments/{id}", id)
            .header("Authorization", "Bearer tokenUser")
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    // ---------- GET /tournaments ----------

    @Test
    @DisplayName ("Should list tournaments without a token, as a non admin")
    void shouldListTournaments_whenAnonymous(){
        TournamentListResponse tournament = new TournamentListResponse(UUID.randomUUID(), "Torneo Nuevo", START, END,
                                                                       new BigDecimal("15.0"), 16);
        when(tournamentService.getTournaments(false)).thenReturn(List.of(tournament));

        restTestClient.get()
            .uri("/tournaments")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].name").isEqualTo("Torneo Nuevo");

        verify(tournamentService).getTournaments(false);
    }

    @Test
    @DisplayName ("Should list tournaments as admin when the user is admin")
    void shouldListTournamentsAsAdmin_whenUserIsAdmin(){
        when(tournamentService.getTournaments(true)).thenReturn(List.of());

        restTestClient.get()
            .uri("/tournaments")
            .header("Authorization", "Bearer tokenAdmin")
            .exchange()
            .expectStatus()
            .isOk();

        verify(tournamentService).getTournaments(true);
    }
}
