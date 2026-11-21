package pmoreno.padelApp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pmoreno.padelApp.model.Tournament;

/**
 * TournamentRepository
 */
public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    
}