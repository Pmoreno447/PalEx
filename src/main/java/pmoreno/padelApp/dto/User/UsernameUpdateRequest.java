package pmoreno.padelApp.dto.User;

import jakarta.validation.constraints.Size;

public record UsernameUpdateRequest(
    @Size (min = 4, max = 20, message = "El nombre de usuario debe tener entre 4 y 20 caracteres")
    String username
) {}
