package pmoreno.padelApp.dto.Court;

import java.math.BigDecimal;
import java.time.LocalTime;

import pmoreno.padelApp.model.Court;

public record CourtResponse(
    Long id,
    String name,
    BigDecimal price,
    int slotMinutes,
    LocalTime openTime,
    LocalTime closeTime
) {
    public static CourtResponse from(Court court){
        return new CourtResponse(
            court.getId(),
            court.getName(), 
            court.getPrice(),
            court.getSlotMinutes(),
            court.getOpenTime(), 
            court.getCloseTime());
    }
}
