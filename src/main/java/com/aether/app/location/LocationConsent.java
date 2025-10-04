package com.aether.app.location;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "location_consent")
public class LocationConsent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean consent;

    @Column(nullable = false, updatable = false)
    private Instant consentGivenAt = Instant.now();

    private Instant consentRevokedAt;

    @Column(length = 255)
    private String userAgent;

    @Column(length = 64)
    private String ipAddress;

    @Column(length = 64)
    private String source;

    @Column(length = 32)
    private String consentVersion;

    @Column(length = 128) private String city;
    @Column(length = 128) private String state;
    @Column(length = 128) private String country;

    private Double latitude;
    private Double longitude;
    private Double accuracyMeters;

    private Instant assertedAt;

    public Long getId() { return id; }

    public boolean isConsent() { return consent; }
    public void setConsent(boolean consent) { this.consent = consent; }

    public Instant getConsentGivenAt() { return consentGivenAt; }
    public void setConsentGivenAt(Instant consentGivenAt) { this.consentGivenAt = consentGivenAt; }

    public Instant getConsentRevokedAt() { return consentRevokedAt; }
    public void setConsentRevokedAt(Instant consentRevokedAt) { this.consentRevokedAt = consentRevokedAt; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getConsentVersion() { return consentVersion; }
    public void setConsentVersion(String consentVersion) { this.consentVersion = consentVersion; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getAccuracyMeters() { return accuracyMeters; }
    public void setAccuracyMeters(Double accuracyMeters) { this.accuracyMeters = accuracyMeters; }

    public Instant getAssertedAt() { return assertedAt; }
    public void setAssertedAt(Instant assertedAt) { this.assertedAt = assertedAt; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    @Transient
    public boolean isRevoked() { return consentRevokedAt != null; }
}