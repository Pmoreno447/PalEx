package pmoreno.padelApp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import static org.springframework.security.oauth2.core.authorization.OAuth2AuthorizationManagers.hasScope;

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
                .requestMatchers(HttpMethod.POST, "/courts").hasRole("ADMIN")
                .anyRequest().permitAll())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt((jwt) -> jwt.jwtAuthenticationConverter(userJwtConverter)))
            .csrf((csrf) -> csrf.disable())
            .build();
    } 
}
