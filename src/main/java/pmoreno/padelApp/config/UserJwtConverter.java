package pmoreno.padelApp.config;

import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import pmoreno.padelApp.model.Role;
import pmoreno.padelApp.model.User;
import pmoreno.padelApp.repository.UserRepository;

@Component 
public class UserJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final UserRepository userRepository;

    public UserJwtConverter(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    //Necesitamos un método personalizado para poder leer
    //Cuales son los permisos que tiene nuestro usuario
    //Los guardamos en la tabla usuarios y no en el token
    //Para mantener la independencia entre proovedores de 
    // seguridad
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        User user = userRepository.findByProviderId(jwt.getSubject())
            .orElseGet(() -> userRepository.save(
                new User(jwt.getSubject(), jwt.getClaimAsString("email"), Role.USER)
            ));

        var authorities = List.of(
            new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        return new JwtAuthenticationToken(jwt, authorities);
    }
}
