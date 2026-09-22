package pmoreno.padelApp.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pmoreno.padelApp.repository.UserRepository;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserRepository userRepository;
    
    public UserController(UserRepository userRepository){
        this.userRepository = userRepository;
    }
}
