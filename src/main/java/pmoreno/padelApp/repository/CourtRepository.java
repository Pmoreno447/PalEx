package pmoreno.padelApp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pmoreno.padelApp.model.Court;

/**
 * CourtRepository
 */
public interface CourtRepository extends JpaRepository<Court, Long> {
    List<Court> findByActiveTrue();
}
