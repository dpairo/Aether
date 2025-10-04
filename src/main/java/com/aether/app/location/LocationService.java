package com.aether.app.location;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class LocationService {

    private final LocationConsentRepository repo;

    public LocationService(LocationConsentRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public LocationConsent saveConsent(LocationConsent toSave) {
        return repo.save(toSave);
    }

    @Transactional(readOnly = true)
    public Optional<LocationConsent> latestActive() {
        return repo.findTopByConsentRevokedAtIsNullOrderByConsentGivenAtDesc();
    }

    @Transactional
    public Optional<LocationConsent> revoke(Long id) {
        return repo.findById(id).map(lc -> {
            lc.setConsentRevokedAt(Instant.now());
            return lc;
        });
    }
}