package pmoreno.padelApp.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import pmoreno.padelApp.model.Tournament;
import pmoreno.padelApp.model.TournamentState;

/**
 * TournamentRepository
 */
public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    Optional<Tournament> findByPublicId(UUID publicId);

    Optional<Tournament> findByPublicIdAndState(UUID publicId, TournamentState tournamentState);

    // Listados ordenados por fecha de comienzo: primero los más próximos
    List<Tournament> findAllByOrderByStartDateAsc();

    List<Tournament> findByStateOrderByStartDateAsc(TournamentState tournamentState);
}