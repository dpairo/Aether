package com.aether.app.infrastructure.web.controller;

import com.aether.app.location.ReverseGeocodingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LocationControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    ReverseGeocodingService reverseGeocodingService;

    @Test
    void postConsent_shouldReturn201_andPersist_withCity() throws Exception {
        given(reverseGeocodingService.reverse(anyDouble(), anyDouble(), any()))
                .willReturn(new ReverseGeocodingService.ReverseResult("Madrid", "Comunidad de Madrid", "España"));

        String body = """
        {
          "consent": true,
          "lat": 40.4168,
          "lon": -3.7038,
          "accuracyMeters": 15.2,
          "source": "browser-geolocation",
          "consentVersion": "v1.0",
          "assertedAtIso": "2025-10-04T12:30:00Z"
        }
        """;

        mvc.perform(post("/api/v1/location/consent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "JUnit/MockClient")
                        .header("Accept-Language", "es")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.consent", is(true)))
                .andExpect(jsonPath("$.lat", is(40.4168)))
                .andExpect(jsonPath("$.lon", is(-3.7038)))
                .andExpect(jsonPath("$.city", is("Madrid")))
                .andExpect(jsonPath("$.state", is("Comunidad de Madrid")))
                .andExpect(jsonPath("$.country", is("España")));

        mvc.perform(get("/api/v1/location/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city", is("Madrid")))
                .andExpect(jsonPath("$.lat", is(40.4168)));
    }

    @Test
    void postConsent_withoutConsent_shouldFail400() throws Exception {
        String body = """
        {
          "consent": false,
          "lat": 40.0,
          "lon": -3.0
        }
        """;

        mvc.perform(post("/api/v1/location/consent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }
}