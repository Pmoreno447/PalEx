package pmoreno.padelApp.config;

import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration 
public class SecurityConfig {
    private final UserJwtConverter userJwtConverter;

    SecurityConfig(UserJwtConverter userJwtConverter) {
        this.userJwtConverter = userJwtConverter;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PathRequest.toH2Console()).permitAll() // solo en dev estoy haciendo pruebas
                .requestMatchers(HttpMethod.GET, "/courts").permitAll()
                .requestMatchers("/users/**").authenticated()
                .anyRequest().hasRole("ADMIN"))
            .oauth2ResourceServer(oauth2 -> oauth2.jwt((jwt) -> jwt.jwtAuthenticationConverter(userJwtConverter)))
            .csrf((csrf) -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())) // la consola H2 usa frames
            .build();
    } 
}
