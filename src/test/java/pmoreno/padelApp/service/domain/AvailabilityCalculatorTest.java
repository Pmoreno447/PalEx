package pmoreno.padelApp.service.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.List;

import org.junit.jupiter.api.Test;

import pmoreno.padelApp.dto.AvailabilityResponse;
import pmoreno.padelApp.repository.projection.OccupiedSlot;

public class AvailabilityCalculatorTest {
    
    @Test 
    void twoDaysWithoutBookings_returnEightBookingsFree(){
        LocalTime openHour = LocalTime.of(16, 0);
        LocalTime closeHour = LocalTime.of(22, 0);
        LocalDateTime from = LocalDateTime.of(LocalDate.of(2026, 8, 9), openHour);
        LocalDateTime to = LocalDateTime.of(LocalDate.of(2026, 8, 10), closeHour);
        int slotTime = 90;
        ArrayDeque<OccupiedSlot> occupiedSlots = new ArrayDeque<>();

        List<AvailabilityResponse> lAvailabilityResponses = 
            AvailabilityCalculator.getAvailability(openHour, closeHour, from, to, slotTime, occupiedSlots);

        assertThat(lAvailabilityResponses)
            .hasSize(8)
            .allMatch(AvailabilityResponse::available);
    }

    @Test
    void twoDaysWithBookings_returnFourBookingsFree(){
        LocalTime openHour = LocalTime.of(16, 0);
        LocalTime closeHour = LocalTime.of(22, 0);
        LocalDateTime from = LocalDateTime.of(LocalDate.of(2026, 8, 9), openHour);
        LocalDateTime to = LocalDateTime.of(LocalDate.of(2026, 8, 10), closeHour);
        int slotTime = 90;
        ArrayDeque<OccupiedSlot> occupiedSlots = new ArrayDeque<>();

        OccupiedSlot o1 = new OccupiedSlot(from, from.plusMinutes(slotTime));
        OccupiedSlot o2 = new OccupiedSlot(from.plusMinutes(slotTime*2), from.plusMinutes(slotTime*3));
        OccupiedSlot o3 = new OccupiedSlot(from.plusDays(1), from.plusDays(1).plusMinutes(slotTime));
        OccupiedSlot o4 = new OccupiedSlot(from.plusDays(1).plusMinutes(slotTime), from.plusDays(1).plusMinutes(slotTime*2));

        occupiedSlots.add(o1);
        occupiedSlots.add(o2);
        occupiedSlots.add(o3);
        occupiedSlots.add(o4);

        List<AvailabilityResponse> lAvailabilityResponses = 
        AvailabilityCalculator.getAvailability(openHour, closeHour, from, to, slotTime, occupiedSlots);

        assertThat(lAvailabilityResponses)
            .hasSize(8)
            .filteredOn(AvailabilityResponse::available)
            .hasSize(4);
    }

    @Test
    void lastSlotOfTheDay_neverExceedsClosingTime(){
        // Con turnos de 90 min de 09:00 a 23:00 el último turno cabe a las 21:00.
        // El de las 22:30 se saldría hasta las 00:00, así que no debe ofrecerse.
        LocalTime openHour = LocalTime.of(9, 0);
        LocalTime closeHour = LocalTime.of(23, 0);
        LocalDate day = LocalDate.of(2026, 8, 9);
        int slotTime = 90;

        List<AvailabilityResponse> slots = AvailabilityCalculator.getAvailability(
            openHour, closeHour, day.atTime(openHour), day.atTime(closeHour), slotTime, new ArrayDeque<>());

        assertThat(slots).hasSize(9);
        assertThat(slots).allMatch(s -> !s.end().isAfter(day.atTime(closeHour)));
        assertThat(slots.get(8).start()).isEqualTo(day.atTime(21, 0));
        assertThat(slots.get(8).end()).isEqualTo(day.atTime(22, 30));
    }

    @Test
    void bookingEndingExactlyWhenSlotStarts_leavesThatSlotFree(){
        LocalTime openHour = LocalTime.of(16, 0);
        LocalTime closeHour = LocalTime.of(22, 0);
        LocalDate day = LocalDate.of(2026, 8, 9);
        int slotTime = 90;

        // Reserva de 16:00 a 17:30: solo ocupa el primer turno.
        ArrayDeque<OccupiedSlot> occupiedSlots = new ArrayDeque<>();
        occupiedSlots.add(new OccupiedSlot(day.atTime(16, 0), day.atTime(17, 30)));

        List<AvailabilityResponse> slots = AvailabilityCalculator.getAvailability(
            openHour, closeHour, day.atTime(openHour), day.atTime(closeHour), slotTime, occupiedSlots);

        assertThat(slots)
            .extracting(AvailabilityResponse::available)
            .containsExactly(false, true, true, true);
    }

    @Test
    void bookingLongerThanSlot_blocksEverySlotItOverlaps(){
        LocalTime openHour = LocalTime.of(16, 0);
        LocalTime closeHour = LocalTime.of(22, 0);
        LocalDate day = LocalDate.of(2026, 8, 9);
        int slotTime = 90;

        // Reserva de 180 min (19:00 a 22:00): pisa los turnos de 19:00 y de 20:30.
        ArrayDeque<OccupiedSlot> occupiedSlots = new ArrayDeque<>();
        occupiedSlots.add(new OccupiedSlot(day.atTime(19, 0), day.atTime(22, 0)));

        List<AvailabilityResponse> slots = AvailabilityCalculator.getAvailability(
            openHour, closeHour, day.atTime(openHour), day.atTime(closeHour), slotTime, occupiedSlots);

        assertThat(slots)
            .extracting(AvailabilityResponse::available)
            .containsExactly(true, true, false, false);
    }

    @Test
    void bookingOnAnotherDay_doesNotAffectTheRequestedDay(){
        LocalTime openHour = LocalTime.of(16, 0);
        LocalTime closeHour = LocalTime.of(22, 0);
        LocalDate day = LocalDate.of(2026, 8, 9);
        int slotTime = 90;

        // La única reserva es del día siguiente, fuera del rango consultado.
        ArrayDeque<OccupiedSlot> occupiedSlots = new ArrayDeque<>();
        occupiedSlots.add(new OccupiedSlot(day.plusDays(1).atTime(16, 0), day.plusDays(1).atTime(17, 30)));

        List<AvailabilityResponse> slots = AvailabilityCalculator.getAvailability(
            openHour, closeHour, day.atTime(openHour), day.atTime(closeHour), slotTime, occupiedSlots);

        assertThat(slots)
            .hasSize(4)
            .allMatch(AvailabilityResponse::available);
    }
}
