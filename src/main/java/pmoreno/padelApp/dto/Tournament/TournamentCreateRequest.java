package pmoreno.padelApp.dto.Tournament;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record TournamentCreateRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    String name,

    // Markdown en bruto: se guarda tal cual y lo renderiza el frontend
    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 10000, message = "La descripción no puede superar los 10000 caracteres")
    String description,

    // Que el fin sea posterior al comienzo se comprueba en el service, como el horario de las pistas
    @NotNull(message = "La fecha de comienzo es obligatoria")
    @Future(message = "El torneo tiene que empezar en el futuro")
    LocalDateTime startDate,

    @NotNull(message = "La fecha de fin es obligatoria")
    LocalDateTime endDate,

    @NotNull(message = "El precio de inscripción es obligatorio")
    @PositiveOrZero(message = "El precio no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "El precio admite como mucho dos decimales")
    BigDecimal entryPrice,

    // Opcional: null significa sin límite de plazas. @Positive deja pasar el null
    @Positive(message = "El número de plazas tiene que ser mayor que 0")
    Integer maxSlots,

    // Solo los ids: el service busca las pistas y falla si alguna no existe. Set para descartar ids repetidos
    @NotEmpty(message = "El torneo tiene que usar al menos una pista")
    Set<@NotNull Long> courtIds
) {}
