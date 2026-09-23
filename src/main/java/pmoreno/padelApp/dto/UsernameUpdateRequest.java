package pmoreno.padelApp.dto;

import jakarta.validation.constraints.Size;

public record UsernameUpdateRequest(
    @Size (min = 4, max = 20)
    String username
) {}
