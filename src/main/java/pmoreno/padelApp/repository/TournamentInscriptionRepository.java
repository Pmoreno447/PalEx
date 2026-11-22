package pmoreno.padelApp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pmoreno.padelApp.model.TournamentRegistration;
import java.util.UUID;


/**
 * TournamentInscription
 */
public interface TournamentInscriptionRepository extends JpaRepository<TournamentRegistration, Long> {

    Optional<TournamentRegistration> findByPublicId(UUID publicId);

    Optional<TournamentRegistration> findByPublicIdAndUserProviderId(UUID publicId, String providerId);

    // Las inscripciones de un usuario, de la más reciente a la más antigua
    List<TournamentRegistration> findByUserProviderIdOrderByRegisteredAtDesc(String providerId);
}