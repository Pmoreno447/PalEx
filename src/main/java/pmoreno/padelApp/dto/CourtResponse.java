package pmoreno.padelApp.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

import pmoreno.padelApp.model.Court;

public record CourtResponse(
    String name,
    BigDecimal price,
    int slotMinutes,
    LocalTime openTime,
    LocalTime closeTime
) {
    public static CourtResponse from(Court court){
        return new CourtResponse(
            court.getName(), 
            court.getPrice(),
            court.getSlotMinutes(),
            court.getOpenTime(), 
            court.getCloseTime());
    }
}
