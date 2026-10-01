package pmoreno.padelApp.dto.Court;

import java.time.LocalDateTime;

public record AvailabilityResponse(
    LocalDateTime start,
    LocalDateTime end,
    boolean available
) { }
