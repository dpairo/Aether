package com.aether.app.strava;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing Strava tokens persistence
 */
@Repository
public interface StravaTokenRepository extends JpaRepository<StravaToken, Long> {
    
    /**
     * Find a token by athlete ID
     */
    Optional<StravaToken> findByAthleteId(Long athleteId);
    
    /**
     * Check if a token exists for a given athlete
     */
    boolean existsByAthleteId(Long athleteId);
}


