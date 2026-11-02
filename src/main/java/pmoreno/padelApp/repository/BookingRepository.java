package pmoreno.padelApp.repository;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pmoreno.padelApp.model.Booking;
import pmoreno.padelApp.model.BookingState;
import pmoreno.padelApp.model.User;
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

// Dos intervalos [a, b) y [c, d) se solapan si a < d y c < b
@Query("""
    select case when count(b) > 0 then true else false end
    from Booking b
    where b.court.id = :courtId
      and b.initDateTime < :end
      and b.endDateTime > :start
      and b.state in :activeStates
""")
    boolean existsOverlapping(Long courtId, LocalDateTime start, LocalDateTime end, Collection<BookingState> activeStates);

    // Solo encuentra la reserva si pertenece al usuario: si no es suya se trata como inexistente
    Optional<Booking> findByPublicIdAndUserProviderId(UUID publicId, String providerId);

    // Optional: el evento de Stripe puede referirse a una reserva que ya no existe (p. ej. tras reiniciar la BD en memoria)
    Optional<Booking> findByPublicId(UUID publicId);

    long countByUserAndState(User user, BookingState state);
}