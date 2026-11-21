package pmoreno.padelApp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pmoreno.padelApp.model.TournamentRegistration;
import java.util.UUID;


/**
 * TournamentInscription
 */
public interface TournamentInscriptionRepository extends JpaRepository<TournamentRegistration, Long> {

    Optional<TournamentRegistration> findByPublicId(UUID publicId);
}