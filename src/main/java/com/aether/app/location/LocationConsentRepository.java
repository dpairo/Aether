package com.aether.app.location;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocationConsentRepository extends JpaRepository<LocationConsent, Long> {
    Optional<LocationConsent> findTopByConsentRevokedAtIsNullOrderByConsentGivenAtDesc();
}