package pmoreno.padelApp.repository.projection;

import java.time.LocalDateTime;

public record OccupiedSlot(
    LocalDateTime start,
    LocalDateTime end
) {
    
}
