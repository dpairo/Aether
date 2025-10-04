package com.aether.app.strava;

import com.aether.app.infrastructure.web.dto.StravaTokenResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.Optional;

/**
 * Service to handle Strava OAuth2 authentication flow
 */
@Service
public class StravaAuthService {
    
    private static final Logger log = LoggerFactory.getLogger(StravaAuthService.class);
    
    private final StravaTokenRepository tokenRepository;
    private final RestTemplate restTemplate;
    
    @Value("${strava.client-id}")
    private String clientId;
    
    @Value("${strava.client-secret}")
    private String clientSecret;
    
    @Value("${strava.redirect-uri}")
    private String redirectUri;
    
    @Value("${strava.auth-url:https://www.strava.com/oauth/authorize}")
    private String authUrl;
    
    @Value("${strava.token-url:https://www.strava.com/oauth/token}")
    private String tokenUrl;
    
    public StravaAuthService(StravaTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * Generate Strava authorization URL
     */
    public String getAuthorizationUrl(String state) {
        return UriComponentsBuilder.fromHttpUrl(authUrl)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("approval_prompt", "auto")
                .queryParam("scope", "read,activity:read_all,profile:read_all")
                .queryParam("state", state)
                .toUriString();
    }
    
    /**
     * Exchange authorization code for access token
     */
    public StravaTokenResponseDTO exchangeCodeForToken(String code) {
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("code", code);
        requestBody.add("grant_type", "authorization_code");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<StravaTokenResponseDTO> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    request,
                    StravaTokenResponseDTO.class
            );
            
            if (response.getBody() != null) {
                log.info("Successfully exchanged code for token. Athlete ID: {}", 
                        response.getBody().athlete().id());
                return response.getBody();
            }
            
            throw new RuntimeException("Empty response from Strava token exchange");
        } catch (Exception e) {
            log.error("Error exchanging code for token: {}", e.getMessage());
            throw new RuntimeException("Failed to exchange authorization code: " + e.getMessage(), e);
        }
    }
    
    /**
     * Refresh an expired access token
     */
    public StravaTokenResponseDTO refreshToken(String refreshToken) {
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("refresh_token", refreshToken);
        requestBody.add("grant_type", "refresh_token");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<StravaTokenResponseDTO> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    request,
                    StravaTokenResponseDTO.class
            );
            
            if (response.getBody() != null) {
                log.info("Successfully refreshed token. Athlete ID: {}", 
                        response.getBody().athlete().id());
                return response.getBody();
            }
            
            throw new RuntimeException("Empty response from Strava token refresh");
        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage());
            throw new RuntimeException("Failed to refresh token: " + e.getMessage(), e);
        }
    }
    
    /**
     * Save or update token in database
     */
    public StravaToken saveToken(StravaTokenResponseDTO tokenResponse) {
        Long athleteId = tokenResponse.athlete().id();
        
        StravaToken token = tokenRepository.findByAthleteId(athleteId)
                .orElse(new StravaToken());
        
        token.setAthleteId(athleteId);
        token.setAccessToken(tokenResponse.accessToken());
        token.setRefreshToken(tokenResponse.refreshToken());
        token.setExpiresAt(Instant.ofEpochSecond(tokenResponse.expiresAt()));
        token.setTokenType(tokenResponse.tokenType());
        
        // Save athlete information
        token.setFirstName(tokenResponse.athlete().firstName());
        token.setLastName(tokenResponse.athlete().lastName());
        token.setUsername(tokenResponse.athlete().username());
        token.setCity(tokenResponse.athlete().city());
        token.setState(tokenResponse.athlete().state());
        token.setCountry(tokenResponse.athlete().country());
        token.setProfileUrl(tokenResponse.athlete().profile());
        
        StravaToken saved = tokenRepository.save(token);
        log.info("Token saved for athlete: {} ({})", 
                athleteId, tokenResponse.athlete().username());
        
        return saved;
    }
    
    /**
     * Get valid token for athlete (refresh if expired)
     */
    public Optional<StravaToken> getValidToken(Long athleteId) {
        Optional<StravaToken> tokenOpt = tokenRepository.findByAthleteId(athleteId);
        
        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }
        
        StravaToken token = tokenOpt.get();
        
        // If token is expired, refresh it
        if (token.isExpired()) {
            log.info("Token expired for athlete {}. Refreshing...", athleteId);
            try {
                StravaTokenResponseDTO refreshed = refreshToken(token.getRefreshToken());
                token = saveToken(refreshed);
                log.info("Token successfully refreshed for athlete {}", athleteId);
            } catch (Exception e) {
                log.error("Failed to refresh token for athlete {}: {}", athleteId, e.getMessage());
                return Optional.empty();
            }
        }
        
        return Optional.of(token);
    }
    
    /**
     * Revoke token (delete from database)
     */
    public void revokeToken(Long athleteId) {
        tokenRepository.findByAthleteId(athleteId)
                .ifPresent(token -> {
                    tokenRepository.delete(token);
                    log.info("Token revoked for athlete: {}", athleteId);
                });
    }
}


