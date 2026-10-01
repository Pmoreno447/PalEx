package pmoreno.padelApp.service.domain;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import pmoreno.padelApp.dto.Court.AvailabilityResponse;
import pmoreno.padelApp.repository.projection.OccupiedSlot;


public class AvailabilityCalculator {

    /**
     * Genera la rejilla de turnos de una pista para un rango de días, marcando
     * cada turno como libre u ocupado.
     *
     * Recorre a la vez la rejilla de turnos (que se genera al vuelo) y la cola de
     * reservas, avanzando siempre hacia delante y sin volver atrás: una reserva que
     * queda por detrás del turno actual no puede afectar a ningún turno posterior,
     * así que se descola. Es el mismo enfoque que el paso de mezcla del mergesort.
     *
     * Complejidad: O(n + m), siendo n el número de turnos del rango y m el de
     * reservas. Cada turno y cada reserva se visitan una sola vez.
     *
     * Precondiciones:
     *   - closeHour posterior a openHour (no se admiten horarios que cruzen la medianoche).
     *   - from con la hora puesta a openHour, y su día no anterior al actual.
     *   - to con la hora puesta a closeHour, y sin superar app.booking.max-days-ahead.
     *   - occupiedSlots ordenada por fecha de inicio ascendente, y con reservas
     *     cuyo inicio cae dentro del rango [from, to].
     */
    public static List<AvailabilityResponse> getAvailability(
        LocalTime openHour,
        LocalTime closeHour,
        LocalDateTime from,
        LocalDateTime to,
        int slotTime,
        ArrayDeque<OccupiedSlot> occupiedSlots
    ){
        List<AvailabilityResponse> response = new ArrayList<>();

        while (from.isBefore(to)) {
            // Si la hora a la que acaba el turno actual es posterior
            // a la hora de cierre, entonces el día ha terminado,
            // pasamos al siguiente día
            // Se compara con LocalDateTime y no con LocalTime porque LocalTime
            // da la vuelta al pasar de las 23:59 y la comparación engañaría
            if(from.plusMinutes(slotTime).isAfter(from.toLocalDate().atTime(closeHour))){
                from = LocalDateTime.of(from.toLocalDate().plusDays(1), openHour);

            }
            else{
                // Si la lista de reservas no está vacía y si el día de la primera reserva
                // es el mismo que el del día que estamos comprobando 
                if(!occupiedSlots.isEmpty() && 
                        from.toLocalDate().isEqual(occupiedSlots.getFirst().start().toLocalDate())){
                        
                    OccupiedSlot slot = occupiedSlots.getFirst();
                    
                    // Si el turno a comprobar va antes que el turno que hay reservado
                    // El turno estará libre y por ende, lo marcamos como libre y avanzamos
                    if(!from.toLocalTime().plusMinutes(slotTime).isAfter(slot.start().toLocalTime())){
                        LocalDateTime starTime = from;
                        from = from.plusMinutes(slotTime);

                        AvailabilityResponse availableSlot = new AvailabilityResponse(starTime, from, true);

                        response.add(availableSlot);
                    }
                    else{
                        // Si el turno con el que estamos comparando ya es posterior 
                        // lo descolamos puesto que ya lo hemos procesado
                        if(!from.toLocalTime().isBefore(slot.end().toLocalTime())){
                            occupiedSlots.remove();
                        }
                        // Si no se cumple ninguno de los casos anteriores, el turno
                        // está ocupado
                        else{
                            LocalDateTime starTime = from;
                            from = from.plusMinutes(slotTime);

                            AvailabilityResponse notAvailableSlot = new AvailabilityResponse(starTime, from, false);

                            response.add(notAvailableSlot);
                        }
                    }

                }
                else{
                    // Si la lista está vacia o ya no quedan más reservas de ese día
                    // el resto de turnos del día están libres
                    while (!from.plusMinutes(slotTime).isAfter(from.toLocalDate().atTime(closeHour))){
                        LocalDateTime starTime = from;
                        from = from.plusMinutes(slotTime);
                        AvailabilityResponse availableSlot = new AvailabilityResponse(starTime, from, true);
                        response.add(availableSlot);
                    }
                }
            }


        }
        return response;
    }

    private  AvailabilityCalculator(){} // Clase de utilidad, no se instancia
}
