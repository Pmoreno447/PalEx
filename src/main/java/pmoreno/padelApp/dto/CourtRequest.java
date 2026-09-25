package pmoreno.padelApp.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import pmoreno.padelApp.model.Court;

public record CourtRequest(
    @Size(min = 1, max = 100, message = "El nombre debe tener entre 1 y 100 caracteres")
    String name,

    @PositiveOrZero(message = "El precio no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "El precio admite como mucho dos decimales")
    BigDecimal price,

    Boolean active,

    @Min(value = 15, message = "Un turno no puede durar menos de 15 minutos")
    @Max(value = 240, message = "Un turno no puede durar más de 4 horas")
    Integer slotMinutes,

    LocalTime openTime,

    LocalTime closTime
) {
    public static CourtRequest from(Court court){
        return new CourtRequest(
            court.getName(), 
            court.getPrice(),
            court.getActive(), 
            court.getSlotMinutes(),
            court.getOpenTime(), 
            court.getCloseTime());
    }

    public Court toModel(){
        return new Court(name, price, active, slotMinutes, openTime, closTime);
    }
}
