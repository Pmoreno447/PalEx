package pmoreno.padelApp.repository;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pmoreno.padelApp.model.Booking;
import pmoreno.padelApp.model.BookingState;
import pmoreno.padelApp.repository.projection.OccupiedSlot;

/**
 * BookingRepository
 */
public interface BookingRepository extends JpaRepository<Booking, Long> {
@Query("""
    select new pmoreno.padelApp.repository.projection.OccupiedSlot(b.initDateTime, b.endDateTime)
    from Booking b
    where b.court.id = :courtId
      and b.initDateTime >= :from
      and b.initDateTime <= :to
      and b.state in :activeStates
    order by b.initDateTime
""")
    ArrayDeque<OccupiedSlot> findOccupiedStarts(Long courtId, LocalDateTime from, LocalDateTime to, Collection<BookingState> activeStates);
}