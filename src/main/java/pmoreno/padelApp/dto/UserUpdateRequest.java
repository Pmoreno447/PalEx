package pmoreno.padelApp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * UserUpdateRequest
 * Este DTO sirve para cambiar
 * valores de la tabla 'users'
 */
public record UserUpdateRequest( 
    @Size (min =1, max= 100) @NotBlank String name,
    @Pattern (regexp = "\\+?[0-9]{9,15}") String phone
) {}