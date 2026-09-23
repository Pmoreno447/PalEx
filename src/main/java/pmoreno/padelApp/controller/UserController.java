package pmoreno.padelApp.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pmoreno.padelApp.dto.UserResponse;
import pmoreno.padelApp.dto.UserUpdateRequest;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;

import pmoreno.padelApp.service.UserService;
import org.springframework.web.bind.annotation.RequestBody;





@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse getMyUser(@AuthenticationPrincipal Jwt jwt) {
        return userService.getMyUser(jwt.getSubject());
    }

    @PatchMapping ("/me")
    public UserResponse updateMyUser(@AuthenticationPrincipal Jwt jwt,
                                    @Valid @RequestBody UserUpdateRequest entity) {
        return userService.updateMyUser(jwt.getSubject(), entity);
    }
    
}


