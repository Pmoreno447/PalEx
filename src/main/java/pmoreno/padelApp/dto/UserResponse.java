package pmoreno.padelApp.dto;

import pmoreno.padelApp.model.User;

/**
 * UserResponse
 */
public record UserResponse(
    String name,
    String email,
    String username,
    String phone 
) {
    public static UserResponse from(User user){
        return new UserResponse(
            user.getName(), 
            user.getEmail(), 
            user.getUsername(),
            user.getPhone());
    }
}