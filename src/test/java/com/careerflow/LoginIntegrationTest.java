package com.careerflow;

import com.careerflow.entity.User;
import com.careerflow.repository.UserRepository;
import com.careerflow.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "CAREERFLOW_DB_TESTS", matches = "true")
class LoginIntegrationTest {
    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        String secret = Base64.getEncoder().encodeToString(Jwts.SIG.HS256.key().build().getEncoded());
        registry.add("app.jwt.secret", () -> secret);
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired JwtService jwt;
    private final String password = "ExamplePass123!";

    private String register(String role) throws Exception {
        String email = "login-test-" + UUID.randomUUID() + "@example.com";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", password, "role", role))))
                .andExpect(status().isCreated());
        return email;
    }
    private String credentials(String email, String password) throws Exception {
        return json.writeValueAsString(Map.of("email", email, "password", password));
    }
    private String login(String email) throws Exception {
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(credentials(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("accessToken").asText();
    }
    @ParameterizedTest @ValueSource(strings = {"JOB_SEEKER", "EMPLOYER"})
    void loginAndMe(String role) throws Exception {
        String email = register(role);
        String token = login(email.toUpperCase());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value(role))
                .andExpect(jsonPath("$.profileId").isNumber())
                .andExpect(jsonPath("$.password").doesNotExist());
    }
    @Test void unknownAndIncorrectCredentialsHaveSameError() throws Exception {
        String email = register("JOB_SEEKER");
        for (String candidate : new String[]{email, "missing-" + email}) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(credentials(candidate, "WrongPassword!")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Invalid email or password"));
        }
    }
    @Test void disabledAccountCannotLoginOrUseExistingToken() throws Exception {
        String email = register("JOB_SEEKER");
        String token = login(email);
        User user = users.findByEmail(email).orElseThrow();
        user.setActive(false);users.saveAndFlush(user);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(credentials(email, password))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
    @Test void missingTokenIsRejected() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
        mvc.perform(get("/api/job-applications")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/jobs/employer/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
    @ParameterizedTest @ValueSource(strings = {"Bearer broken", "Bearer ", "Basic abc"})
    void invalidHeadersAreRejected(String header) throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", header)).andExpect(status().isUnauthorized());
    }
    @Test void tokenForMissingUserIsRejected() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + jwt.issue(Long.MAX_VALUE)))
                .andExpect(status().isUnauthorized());
    }
    @Test void authenticatedRequestDoesNotCreateSession() throws Exception {
        String token = login(register("JOB_SEEKER"));
        var result = mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }
    @Test void malformedLoginReturns400() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
    @Test void publicRoutesRemainAccessible() throws Exception {
        mvc.perform(get("/api/jobs")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }
    @Test void loginIgnoresOldInvalidBearerToken() throws Exception {
        String email = register("JOB_SEEKER");
        mvc.perform(post("/api/auth/login").servletPath("/api/auth/login")
                .header("Authorization", "Bearer expired")
                .contentType(MediaType.APPLICATION_JSON).content(credentials(email, password)))
                .andExpect(status().isOk());
    }
}
