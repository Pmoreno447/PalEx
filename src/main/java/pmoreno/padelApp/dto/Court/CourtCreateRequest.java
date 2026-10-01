package pmoreno.padelApp.dto.Court;

import java.math.BigDecimal;
import java.time.LocalTime;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import pmoreno.padelApp.model.Court;

public record CourtCreateRequest(
    @Size(min = 1, max = 100, message = "El nombre debe tener entre 1 y 100 caracteres")
    String name,

    @NotNull(message = "El precio es obligatorio")
    @PositiveOrZero(message = "El precio no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "El precio admite como mucho dos decimales")
    BigDecimal price,

    @NotNull(message = "Hay que indicar si la pista está activa")
    Boolean active,

    @NotNull(message = "La duración del turno es obligatoria")
    @Min(value = 15, message = "Un turno no puede durar menos de 15 minutos")
    @Max(value = 240, message = "Un turno no puede durar más de 4 horas")
    Integer slotMinutes,

    @NotNull(message = "La hora de apertura es obligatoria")
    LocalTime openTime,

    @NotNull(message = "La hora de cierre es obligatoria")
    LocalTime closTime
) {
    public static CourtCreateRequest from(Court court){
        return new CourtCreateRequest(
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
