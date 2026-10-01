package pmoreno.padelApp.dto.User;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * UserUpdateRequest
 * Este DTO sirve para cambiar
 * valores de la tabla 'users'
 */
public record UserUpdateRequest( 
    @Size (min =1, max= 100, message = "El nombre debe tener entre 1 y 100 caracteres")
    @Pattern (regexp = ".*\\S.*", message = "El nombre no puede estar vacío")
    String name,

    @Pattern (regexp = "\\+?[0-9]{9,15}", message = "Número de teléfono no valido")
    String phone
) {}