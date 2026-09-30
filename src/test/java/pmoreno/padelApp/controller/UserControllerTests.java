package pmoreno.padelApp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import pmoreno.padelApp.service.UserService;
import pmoreno.padelApp.config.SecurityConfig;
import pmoreno.padelApp.config.UserJwtConverter;
import pmoreno.padelApp.dto.UserResponse;
import pmoreno.padelApp.dto.UserUpdateRequest;
import pmoreno.padelApp.exceptions.ResourceNotFoundException;

@Import (SecurityConfig.class)
@EnableConfigurationProperties(H2ConsoleProperties.class)
@WebMvcTest({UserController.class})
@AutoConfigureRestTestClient
public class UserControllerTests {
    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private  UserJwtConverter userJwtConverter;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private Jwt jwt;

    @BeforeEach
    void setUp(){
        jwt = Jwt.withTokenValue("token").header("alg", "none").subject("user-1").build();

        // Todas las peticiones van con "Bearer token" de un usuario con ROLE_USER
        when(jwtDecoder.decode("token")).thenReturn(jwt);
        when(userJwtConverter.convert(jwt))
            .thenReturn(new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @Test
    @DisplayName("Should return ok")
    void shouldGetOk_whenUserExist(){
        when(userService.getMyUser("user-1"))
            .thenReturn(new UserResponse("Test Name", "test@example.com", "CoolTestUser", "+34123456789"));

        restTestClient.get()
            .uri("/users/me")
            .header("Authorization", "Bearer token")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
                .jsonPath("$.name").isEqualTo("Test Name")
                .jsonPath("$.email").isEqualTo("test@example.com")
                .jsonPath("$.username").isEqualTo("CoolTestUser")
                .jsonPath("$.phone").isEqualTo("+34123456789");

        // El controller pasa al service el subject del token
        verify(userService).getMyUser("user-1");
    }

    @Test
    @DisplayName("Should return not found when the user does not exist")
    void shouldGetNotFound_whenUserDoesNotExist(){
        when(userService.getMyUser("user-1"))
            .thenThrow(new ResourceNotFoundException("Usuario user-1 no sincronizado con la base de datos."));

        restTestClient.get()
            .uri("/users/me")
            .header("Authorization", "Bearer token")
            .exchange()
            .expectStatus().isNotFound()
            .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.detail").isEqualTo("Usuario user-1 no sincronizado con la base de datos.");
    }

    @Test
    @DisplayName("Should update the user fields")
    void shouldUpdateUser_whenRequestIsValid(){
        UserUpdateRequest request = new UserUpdateRequest("New Name", "+34987654321");
        when(userService.updateMyUser("user-1", request))
            .thenReturn(new UserResponse("New Name", "test@example.com", "CoolTestUser", "+34987654321"));

        restTestClient.patch()
            .uri("/users/me")
            .header("Authorization", "Bearer token")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {"name": "New Name", "phone": "+34987654321"}
                """)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
                .jsonPath("$.name").isEqualTo("New Name")
                .jsonPath("$.phone").isEqualTo("+34987654321");

        // El JSON se convierte en el UserUpdateRequest esperado y llega al service con el subject del token
        verify(userService).updateMyUser("user-1", request);
    }

    @Test
    @DisplayName("Should return bad request when the phone is not valid")
    void shouldGetBadRequest_whenPhoneIsNotValid(){
        restTestClient.patch()
            .uri("/users/me")
            .header("Authorization", "Bearer token")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {"phone": "abc"}
                """)
            .exchange()
            .expectStatus().isBadRequest();

        // @Valid corta la petición antes de llegar al service
        verify(userService, never()).updateMyUser(any(), any());
    }
}
