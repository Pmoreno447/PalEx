package pmoreno.padelApp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

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
        when(tournamentService.createTournament(any()))
            .thenReturn(new TournamentResponse(1L, "Torneo Nuevo", "**Torneo** de prueba", START, END,
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
                .valueMatches("Location", ".*/tournaments/1")
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
}
