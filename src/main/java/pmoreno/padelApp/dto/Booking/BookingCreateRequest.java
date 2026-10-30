package pmoreno.padelApp.dto.Booking;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

/**
 * BookingRequest
 */
public record BookingCreateRequest(
    @NotNull(message = "Hay que indicar una pista.")
    Long courtId,

    @NotNull(message = "La hora de inicio es obligatoria")
    LocalDateTime initTime
) { }