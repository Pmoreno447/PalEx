package pmoreno.padelApp.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import pmoreno.padelApp.model.Tournament;

/**
 * TournamentRepository
 */
public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    Optional<Tournament> findByPublicId(UUID publicId);
}