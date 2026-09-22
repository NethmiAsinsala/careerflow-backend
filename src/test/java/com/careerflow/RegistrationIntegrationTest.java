package com.careerflow;

import com.careerflow.entity.User;
import com.careerflow.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Opt in with CAREERFLOW_DB_TESTS=true against a local development database.
 * Each test rolls back its rows; MySQL auto-increment counters may still advance. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "CAREERFLOW_DB_TESTS", matches = "true")
class RegistrationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @MockitoSpyBean JobSeekerRepository seekers;
    @Autowired EmployerRepository employers;
    @Autowired PasswordEncoder encoder;

    private String email() { return "registration-test-" + UUID.randomUUID() + "@example.com"; }
    private String body(String email, String password, String role) throws Exception {
        return json.writeValueAsString(Map.of("email", email, "password", password, "role", role));
    }

    @ParameterizedTest
    @ValueSource(strings = {"JOB_SEEKER", "EMPLOYER"})
    void registersWithHashedPasswordAndMatchingProfile(String role) throws Exception {
        String email = email();
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body(email.toUpperCase(), "ExamplePass123!", role)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value(role))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.profileId").isNumber())
                .andReturn().getResponse().getContentAsString();
        User user = users.findByEmail(email).orElseThrow();
        assertThat(user.getPassword()).isNotEqualTo("ExamplePass123!");
        assertThat(encoder.matches("ExamplePass123!", user.getPassword())).isTrue();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(user.getActive()).isTrue();
        long profileId = json.readTree(response).get("profileId").asLong();
        if (role.equals("JOB_SEEKER")) {
            assertThat(seekers.findById(profileId).orElseThrow().getUser().getId()).isEqualTo(user.getId());
            assertThat(employers.existsByUserId(user.getId())).isFalse();
        } else {
            assertThat(employers.findById(profileId).orElseThrow().getUser().getId()).isEqualTo(user.getId());
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rollsBackUserIfProfileCreationFails() throws Exception {
        String email = email();
        doThrow(new IllegalStateException("Simulated profile failure")).when(seekers).save(any());
        String request = body(email, "ExamplePass123!", "JOB_SEEKER");
        assertThatThrownBy(() -> mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(request)))
                .hasRootCauseMessage("Simulated profile failure");
        assertThat(users.existsByEmail(email)).isFalse();
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase() throws Exception {
        String email = email();
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body(email, "ExamplePass123!", "JOB_SEEKER"))).andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body(email.toUpperCase(), "ExamplePass123!", "EMPLOYER")))
                .andExpect(status().isConflict());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "OTHER", "", "job_seeker"})
    void rejectsNonPublicRoles(String role) throws Exception {
        String email = email();
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body(email, "ExamplePass123!", role)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.role").exists());
        assertThat(users.existsByEmail(email)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"short", "", "        "})
    void rejectsInvalidPasswords(String password) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body(email(), password, "JOB_SEEKER")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void rejectsOversizeUnicodePassword() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body(email(), "\u00e9".repeat(40), "JOB_SEEKER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidEmail() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body("invalid", "ExamplePass123!", "JOB_SEEKER")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void rejectsMissingFields() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMalformedJsonWithoutEchoingBody() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A valid JSON body is required"));
    }
}
