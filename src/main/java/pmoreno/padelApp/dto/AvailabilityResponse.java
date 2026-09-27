package pmoreno.padelApp.dto;

import java.time.LocalDateTime;

public record AvailabilityResponse(
    LocalDateTime start,
    LocalDateTime end,
    boolean available
) { }
