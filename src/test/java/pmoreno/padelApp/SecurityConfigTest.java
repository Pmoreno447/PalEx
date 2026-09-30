package pmoreno.padelApp;

import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.h2console.autoconfigure.H2ConsoleProperties;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import pmoreno.padelApp.config.SecurityConfig;
import pmoreno.padelApp.config.UserJwtConverter;
import pmoreno.padelApp.controller.CourtController;
import pmoreno.padelApp.controller.UserController;
import pmoreno.padelApp.dto.CourtRequest;
import pmoreno.padelApp.service.CourtService;
import pmoreno.padelApp.service.UserService;

@Import (SecurityConfig.class)
@EnableConfigurationProperties(H2ConsoleProperties.class)
@WebMvcTest({CourtController.class, UserController.class}) 
@AutoConfigureRestTestClient 
public class SecurityConfigTest {

    @Autowired 
    private RestTestClient restTestClient;

    @MockitoBean 
    private CourtService courtService;

    @MockitoBean 
    private UserService userService;

    @MockitoBean
    private UserJwtConverter userJwtConverter;

    @MockitoBean 
    private JwtDecoder jwtDecoder;

    @Test
    void getErrorWithoutUserToken(){ 
        restTestClient.get().uri("/users/me").exchange()
            .expectStatus()
            .isUnauthorized();
    }


    @Test
    void getOkWithUserToken(){
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("user-1").build();
        when(jwtDecoder.decode("token")).thenReturn(jwt);
        when(userJwtConverter.convert(jwt))
            .thenReturn(new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        restTestClient.get().uri("/users/me")
            .header("Authorization", "Bearer token")
            .exchange()
            .expectStatus()
            .isOk();
    }

    
        @Test
        void getAvailabilityWithoutToken(){
            restTestClient.get().uri("/courts/1/availability?from=2026-10-01&to=2026-10-07")
                .exchange()
                .expectStatus()
                .isOk();
        }

        @Test
        void getOtherEndpointWithoutAdminToken(){
            CourtRequest courtRequest = new CourtRequest(null, null, null, null, null, null);

            restTestClient.post().uri("/courts")
                .body(courtRequest)
                .exchange()
                .expectStatus()
                .isUnauthorized();
        }

        @Test
        void getOtherEndpointWithUserToken(){
            Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("user-1").build();
            when(jwtDecoder.decode("token")).thenReturn(jwt);
            when(userJwtConverter.convert(jwt))
                .thenReturn(new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

            CourtRequest courtRequest = new CourtRequest(null, null, null, null, null, null);

             restTestClient.post().uri("/courts")
                .header("Authorization", "Bearer token")
                .body(courtRequest)
                .exchange()
                .expectStatus()
                .isForbidden();
        }
}
