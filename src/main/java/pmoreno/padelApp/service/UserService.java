package pmoreno.padelApp.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pmoreno.padelApp.dto.UserResponse;
import pmoreno.padelApp.dto.UserUpdateRequest;
import pmoreno.padelApp.exceptions.ResourceNotFoundException;
import pmoreno.padelApp.model.User;
import pmoreno.padelApp.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService (UserRepository userRepository){
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getMyUser(String providerId) {
        return userRepository.findByProviderId(providerId)
                .map(UserResponse::from)
                .orElseThrow(()-> new ResourceNotFoundException("Usuario " +providerId +  " no sincronizado con la base de datos."));
    }

    @Transactional
    public UserResponse updateMyUser(String providerId, UserUpdateRequest userUpdate){
        User user = userRepository.findByProviderId(providerId)
        .orElseThrow(() -> new ResourceNotFoundException("Usuario " +providerId +  " no sincronizado con la base de datos."));
        
        if(userUpdate.name() != null){
            user.setName(userUpdate.name().trim());
        }

        if(userUpdate.phone() != null){
            user.setPhone(userUpdate.phone());
        }

        return UserResponse.from(user);
    }
}
