package pmoreno.padelApp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pmoreno.padelApp.dto.User.UserResponse;
import pmoreno.padelApp.dto.User.UserUpdateRequest;
import pmoreno.padelApp.exceptions.ResourceNotFoundException;
import pmoreno.padelApp.model.Role;
import pmoreno.padelApp.model.User;
import pmoreno.padelApp.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class UserServiceTests {
    @Mock 
    private UserRepository userRepository;
    
    @InjectMocks  
    private UserService userService;

    private User userTest;   
    
    @BeforeEach 
    void setUp(){
        userTest = new User(1L, "Old Name", "providerTestId", "test@example.com", "CoolTestUser", "+34123456789", Role.USER);
    }

    @Test
    @DisplayName ("Should update only the params included in userUpdateRequest")
    void shouldUpdateUser(){
        when(userRepository.findByProviderId("providerTestId")).thenReturn(Optional.of(userTest));

        UserUpdateRequest updateRequest = new UserUpdateRequest("New Name", "+34987654321");
        UserResponse userResponse = userService.updateMyUser("providerTestId", updateRequest);

        // Solo se actualizan los valores que corresponden
        assertEquals("New Name", userResponse.name());
        assertEquals("+34987654321", userResponse.phone());
        
        // El resto de valores sigue igual
        assertEquals("test@example.com", userResponse.email());
        assertEquals("CoolTestUser", userResponse.username());
    }

    @Test
    @DisplayName ("Should keep old values when the request fields are null")
    void shouldKeepValuesWhenNull(){
        when(userRepository.findByProviderId("providerTestId")).thenReturn(Optional.of(userTest));

        UserResponse userResponse = userService.updateMyUser("providerTestId", new UserUpdateRequest(null, null));

        assertEquals("Old Name", userResponse.name());
        assertEquals("+34123456789", userResponse.phone());
    }

    @Test
    @DisplayName ("Should update only the name")
    void shouldUpdateOnlyName(){
        when(userRepository.findByProviderId("providerTestId")).thenReturn(Optional.of(userTest));

        UserResponse userResponse = userService.updateMyUser("providerTestId", new UserUpdateRequest("New Name", null));

        assertEquals("New Name", userResponse.name());
        assertEquals("+34123456789", userResponse.phone());
    }

    @Test
    @DisplayName ("Should update only the phone")
    void shouldUpdateOnlyPhone(){
        when(userRepository.findByProviderId("providerTestId")).thenReturn(Optional.of(userTest));

        UserResponse userResponse = userService.updateMyUser("providerTestId", new UserUpdateRequest(null, "+34987654321"));

        assertEquals("Old Name", userResponse.name());
        assertEquals("+34987654321", userResponse.phone());
    }

    @Test
    @DisplayName ("Should trim the name")
    void shouldTrimName(){
        when(userRepository.findByProviderId("providerTestId")).thenReturn(Optional.of(userTest));

        UserResponse userResponse = userService.updateMyUser("providerTestId", new UserUpdateRequest("  New Name  ", null));

        assertEquals("New Name", userResponse.name());
    }

    @Test
    @DisplayName ("Should return the correct user")
    void shouldReturnCorrectUser(){
        when(userRepository.findByProviderId("providerTestId")).thenReturn(Optional.of(userTest));

        UserResponse userResponse = userService.getMyUser("providerTestId");

        assertEquals("Old Name", userResponse.name());
        assertEquals("+34123456789", userResponse.phone());
        assertEquals("test@example.com", userResponse.email());
        assertEquals("CoolTestUser", userResponse.username());
    }

    @Test 
    @DisplayName ("Should not update nothing")
    void shoulNotUpdateNothing(){
        UserUpdateRequest updateRequest = new UserUpdateRequest("New Name", "+34987654321");

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> 
            {userService.updateMyUser("noProviderTestId", updateRequest);}
        );
    
        assertEquals("Usuario noProviderTestId no sincronizado con la base de datos.", exception.getMessage());
    }

    @Test
    @DisplayName ("Should not return a user")
    void shouldNotReturntUser(){
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> 
            {userService.getMyUser("noProviderTestId");}
        );
    
        assertEquals("Usuario noProviderTestId no sincronizado con la base de datos.", exception.getMessage());
    }

}
