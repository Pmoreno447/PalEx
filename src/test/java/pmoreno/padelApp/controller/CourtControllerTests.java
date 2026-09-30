package pmoreno.padelApp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
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
import static org.assertj.core.api.Assertions.assertThat;


import pmoreno.padelApp.config.SecurityConfig;
import pmoreno.padelApp.config.UserJwtConverter;
import pmoreno.padelApp.dto.AvailabilityResponse;
import pmoreno.padelApp.dto.CourtRequest;
import pmoreno.padelApp.dto.CourtResponse;
import pmoreno.padelApp.exceptions.BadRequestException;
import pmoreno.padelApp.exceptions.ResourceNotFoundException;
import pmoreno.padelApp.service.CourtService;

@Import (SecurityConfig.class)
@EnableConfigurationProperties (H2ConsoleProperties.class)
@WebMvcTest({CourtController.class})
@AutoConfigureRestTestClient
public class CourtControllerTests {
    private static final String INVALID_HOURS_MESSAGE = "La hora de cierre no puede ser anterior a la de apertura";

    // Pista válida: la misma en JSON (lo que manda el cliente) y como record (lo que debe llegar al service)
    private static final String COURT_JSON = """
        {"name": "Pista Nueva", "price": 5.0, "active": true, "slotMinutes": 90,
         "openTime": "16:00", "closTime": "22:00"}
        """;
    private static final CourtRequest COURT_REQUEST = new CourtRequest("Pista Nueva", new BigDecimal("5.0"), true, 90,
                                                                       LocalTime.of(16, 0), LocalTime.of(22, 0));

    // Pista con la apertura posterior al cierre
    private static final String INVALID_HOURS_JSON = """
        {"name": "Pista Nueva", "price": 5.0, "active": true, "slotMinutes": 90,
         "openTime": "22:00", "closTime": "16:00"}
        """;

    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private CourtService courtService;

    @MockitoBean
    private UserJwtConverter userJwtConverter;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private Jwt userJwt;

    private Jwt adminJwt;

    private List<CourtResponse> courtListUser;

    private List<CourtResponse> courtListAdmin;

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


        // Pistas visibles para un usuario
        courtListUser = new ArrayList<>();
        CourtResponse c1 = new CourtResponse("Pista 1", BigDecimal.ONE, 90, LocalTime.of(16, 0), LocalTime.of(22, 0));
        CourtResponse c2 = new CourtResponse("Pista 2", BigDecimal.TWO, 180, LocalTime.of(16, 0), LocalTime.of(22, 0));
        CourtResponse c3 = new CourtResponse("Pista 3", BigDecimal.TEN, 90, LocalTime.of(8, 0), LocalTime.of(16, 0));
        CourtResponse c4 = new CourtResponse("Pista 4", BigDecimal.TWO, 180, LocalTime.of(8, 0), LocalTime.of(16, 0));

        Collections.addAll(courtListUser, c1, c2, c3, c4);

        // Pistas visibles para un administrador
        courtListAdmin = new ArrayList<>();
        CourtResponse c5 = new CourtResponse("Pista 5", BigDecimal.TWO, 90, LocalTime.of(16, 0), LocalTime.of(22, 0));

        courtListAdmin.addAll(courtListUser);
        courtListAdmin.add(c5);
    }

    // ---------- GET /courts ----------

    @Test
    @DisplayName ("Should return only activated courts")
    void shouldReturnNoAdminList(){
        when(courtService.getCourts(true)).thenReturn(courtListAdmin);
        when(courtService.getCourts(false)).thenReturn(courtListUser);

        restTestClient.get()
            .uri("/courts")
            .header("Authorization", "Bearer tokenUser")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
                .jsonPath("$[*].name").value(names ->
                    assertThat((List<String>) names).containsExactlyInAnyOrder("Pista 1", "Pista 2", "Pista 3", "Pista 4"));
    }

    @Test
    @DisplayName ("Should return every court for an admin")
    void shouldReturnAdminList_whenUserIsAdmin(){
        when(courtService.getCourts(true)).thenReturn(courtListAdmin);
        when(courtService.getCourts(false)).thenReturn(courtListUser);

        restTestClient.get()
            .uri("/courts")
            .header("Authorization", "Bearer tokenAdmin")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
                .jsonPath("$[*].name").value(names ->
                    assertThat((List<String>) names)
                        .containsExactlyInAnyOrder("Pista 1", "Pista 2", "Pista 3", "Pista 4", "Pista 5"));

        // El controller detecta el ROLE_ADMIN y se lo indica al service
        verify(courtService).getCourts(true);
    }

    // ---------- POST /courts ----------

    @Test
    @DisplayName ("Should forbid creating a court when the user is not admin")
    void shouldForbidCreateCourt_whenUserIsNotAdmin(){
        restTestClient.post()
            .uri("/courts")
            .header("Authorization", "Bearer tokenUser")
            .contentType(MediaType.APPLICATION_JSON)
            .body(COURT_JSON)
            .exchange()
            .expectStatus()
            .isForbidden();

        verify(courtService, never()).createCourt(any());
    }

    @Test
    @DisplayName ("Should create a court when the user is admin")
    void shouldCreateCourt_whenUserIsAdmin(){
        when(courtService.createCourt(COURT_REQUEST))
            .thenReturn(new CourtResponse("Pista Nueva", new BigDecimal("5.0"), 90, LocalTime.of(16, 0), LocalTime.of(22, 0)));

        restTestClient.post()
            .uri("/courts")
            .header("Authorization", "Bearer tokenAdmin")
            .contentType(MediaType.APPLICATION_JSON)
            .body(COURT_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
                .jsonPath("$.name").isEqualTo("Pista Nueva")
                .jsonPath("$.price").isEqualTo(5.0)
                .jsonPath("$.slotMinutes").isEqualTo(90)
                .jsonPath("$.openTime").isEqualTo("16:00:00")
                .jsonPath("$.closeTime").isEqualTo("22:00:00");

        // El JSON se convierte en el CourtRequest esperado
        verify(courtService).createCourt(COURT_REQUEST);
    }

    @Test
    @DisplayName ("Should return bad request when an admin creates a court with invalid hours")
    void shouldGetBadRequest_whenAdminCreatesCourtWithInvalidHours(){
        // La regla vive en el service (ya probada en CourtServiceTests): aquí se comprueba que llega como 400
        when(courtService.createCourt(any())).thenThrow(new BadRequestException(INVALID_HOURS_MESSAGE));

        restTestClient.post()
            .uri("/courts")
            .header("Authorization", "Bearer tokenAdmin")
            .contentType(MediaType.APPLICATION_JSON)
            .body(INVALID_HOURS_JSON)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.detail").isEqualTo(INVALID_HOURS_MESSAGE);
    }

    // ---------- PATCH /courts/{id} ----------

    @Test
    @DisplayName ("Should forbid updating a court when the user is not admin")
    void shouldForbidUpdateCourt_whenUserIsNotAdmin(){
        restTestClient.patch()
            .uri("/courts/1")
            .header("Authorization", "Bearer tokenUser")
            .contentType(MediaType.APPLICATION_JSON)
            .body(COURT_JSON)
            .exchange()
            .expectStatus()
            .isForbidden();

        verify(courtService, never()).updateCourt(any(), any());
    }

    @Test
    @DisplayName ("Should update a court when the user is admin")
    void shouldUpdateCourt_whenUserIsAdmin(){
        when(courtService.updateCourt(1L, COURT_REQUEST))
            .thenReturn(new CourtResponse("Pista Nueva", new BigDecimal("5.0"), 90, LocalTime.of(16, 0), LocalTime.of(22, 0)));

        restTestClient.patch()
            .uri("/courts/1")
            .header("Authorization", "Bearer tokenAdmin")
            .contentType(MediaType.APPLICATION_JSON)
            .body(COURT_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
                .jsonPath("$.name").isEqualTo("Pista Nueva")
                .jsonPath("$.openTime").isEqualTo("16:00:00")
                .jsonPath("$.closeTime").isEqualTo("22:00:00");

        // El id de la URL y el JSON llegan al service
        verify(courtService).updateCourt(1L, COURT_REQUEST);
    }

    @Test
    @DisplayName ("Should return bad request when an admin updates a court with invalid hours")
    void shouldGetBadRequest_whenAdminUpdatesCourtWithInvalidHours(){
        when(courtService.updateCourt(eq(1L), any())).thenThrow(new BadRequestException(INVALID_HOURS_MESSAGE));

        restTestClient.patch()
            .uri("/courts/1")
            .header("Authorization", "Bearer tokenAdmin")
            .contentType(MediaType.APPLICATION_JSON)
            .body(INVALID_HOURS_JSON)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.detail").isEqualTo(INVALID_HOURS_MESSAGE);
    }

    // ---------- GET /courts/{id}/availability (público, sin token) ----------

    @Test
    @DisplayName ("Should return bad request when the date range is not valid")
    void shouldGetBadRequest_whenDateRangeIsNotValid(){
        LocalDate yesterday = LocalDate.now().minusDays(1);
        when(courtService.getCourtDisponibility(1L, yesterday, yesterday))
            .thenThrow(new BadRequestException("Rango de fechas no válido"));

        restTestClient.get()
            .uri("/courts/1/availability?from={from}&to={to}", yesterday, yesterday)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody()
                .jsonPath("$.detail").isEqualTo("Rango de fechas no válido");
    }

    @Test
    @DisplayName ("Should return not found when the court does not exist")
    void shouldGetNotFound_whenCourtDoesNotExist(){
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        when(courtService.getCourtDisponibility(99L, tomorrow, tomorrow))
            .thenThrow(new ResourceNotFoundException("Pista con id 99 no encontrada."));

        restTestClient.get()
            .uri("/courts/99/availability?from={from}&to={to}", tomorrow, tomorrow)
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectBody()
                .jsonPath("$.detail").isEqualTo("Pista con id 99 no encontrada.");
    }

    @Test
    @DisplayName ("Should return not found when the court is not active")
    void shouldGetNotFound_whenCourtIsNotActive(){
        // El service trata una pista inactiva como inexistente (ADR-006), así que la respuesta es la misma
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        when(courtService.getCourtDisponibility(5L, tomorrow, tomorrow))
            .thenThrow(new ResourceNotFoundException("Pista con id 5 no encontrada."));

        restTestClient.get()
            .uri("/courts/5/availability?from={from}&to={to}", tomorrow, tomorrow)
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectBody()
                .jsonPath("$.detail").isEqualTo("Pista con id 5 no encontrada.");
    }

    @Test
    @DisplayName ("Should return the availability of an active court")
    void shouldReturnAvailability_whenCourtIsActive(){
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        when(courtService.getCourtDisponibility(1L, tomorrow, tomorrow)).thenReturn(List.of(
            new AvailabilityResponse(tomorrow.atTime(16, 0), tomorrow.atTime(17, 30), true),
            new AvailabilityResponse(tomorrow.atTime(17, 30), tomorrow.atTime(19, 0), false)));

        restTestClient.get()
            .uri("/courts/1/availability?from={from}&to={to}", tomorrow, tomorrow)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].start").isEqualTo(tomorrow + "T16:00:00")
                .jsonPath("$[0].end").isEqualTo(tomorrow + "T17:30:00")
                .jsonPath("$[0].available").isEqualTo(true)
                .jsonPath("$[1].available").isEqualTo(false);

        // Los parámetros de la URL llegan convertidos a LocalDate
        verify(courtService).getCourtDisponibility(1L, tomorrow, tomorrow);
    }
}
