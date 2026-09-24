package com.careerflow;

import com.careerflow.entity.*;
import com.careerflow.repository.*;
import com.careerflow.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(WorkflowIntegrationTest.FixedTime.class)
@EnabledIfEnvironmentVariable(named = "CAREERFLOW_DB_TESTS", matches = "true")
class WorkflowIntegrationTest {
    @TestConfiguration
    static class FixedTime {
        @Bean @Primary Clock testClock() {
            return Clock.fixed(Instant.parse("2030-01-01T06:30:00Z"), ZoneId.of("Asia/Colombo"));
        }
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        String secret = Base64.getEncoder().encodeToString(io.jsonwebtoken.Jwts.SIG.HS256.key().build().getEncoded());
        registry.add("app.jwt.secret", () -> secret);
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtService jwt;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired EmployerRepository employers;
    @Autowired JobSeekerRepository seekers;
    @Autowired JobRepository jobs;
    @Autowired JobApplicationRepository applications;
    private final LocalDateTime now = LocalDateTime.of(2030, 1, 1, 12, 0);
    private User employerUser, candidateUser;
    private Employer employer;
    private JobSeeker candidate;
    private Job job;
    private User account(String role) {
        User user = new User();user.setEmail("workflow-test-" + UUID.randomUUID() + "@example.com");
        user.setPassword("unused-test-hash");user.setRole(roles.findByName(role).orElseThrow());return users.saveAndFlush(user);
    }
    @BeforeEach void setup() {
        employerUser = account("EMPLOYER");candidateUser = account("JOB_SEEKER");
        employer = employers.saveAndFlush(Employer.builder().user(employerUser).build());
        candidate = new JobSeeker();candidate.setUser(candidateUser);candidate = seekers.saveAndFlush(candidate);
        job = jobs.saveAndFlush(Job.builder().employer(employer).title("Test job").status("OPEN").applicationDeadline(now.plusDays(1)).build());
    }
    private ResultActions call(User user, MockHttpServletRequestBuilder request) throws Exception {
        if (user != null) request.header("Authorization", "Bearer " + jwt.issue(user.getId()));
        return mvc.perform(request);
    }
    private MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder request, Map<String, ?> values) throws Exception {
        return request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(values));
    }
    private String applyPath(Long jobId) {
        return "/api/job-applications/job/" + jobId + "/job-seeker/" + candidate.getId();
    }
    private JobApplication application(String status) {
        return applications.saveAndFlush(JobApplication.builder().job(job).jobSeeker(candidate).status(status).build());
    }
    @Test void openJobAcceptsApplicationAndDuplicateIs409() throws Exception {
        call(candidateUser, body(post(applyPath(job.getId())), Map.of())).andExpect(status().isCreated());
        call(candidateUser, body(post(applyPath(job.getId())), Map.of())).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Job seeker has already applied for this job"));
        assertThat(applications.findByJobId(job.getId())).hasSize(1);
    }
    @ParameterizedTest @ValueSource(strings = {"CLOSED", "ARCHIVED"})
    void nonOpenJobsRejectApplications(String status) throws Exception {
        job.setStatus(status);jobs.saveAndFlush(job);
        call(candidateUser, body(post(applyPath(job.getId())), Map.of())).andExpect(status().isConflict());
        assertThat(applications.findByJobId(job.getId())).isEmpty();
    }
    @ParameterizedTest @ValueSource(ints = {-1, 0})
    void expiredOrExactDeadlineRejectsApplications(int seconds) throws Exception {
        job.setApplicationDeadline(now.plusSeconds(seconds));jobs.saveAndFlush(job);
        call(candidateUser, body(post(applyPath(job.getId())), Map.of())).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("The application deadline has passed"));
        assertThat(applications.findByJobId(job.getId())).isEmpty();
    }
    @Test void noDeadlineAcceptsApplications() throws Exception {
        job.setApplicationDeadline(null);jobs.saveAndFlush(job);
        call(candidateUser, body(post(applyPath(job.getId())), Map.of())).andExpect(status().isCreated());
    }
    @Test void missingJobIs404() throws Exception {
        call(candidateUser, body(post(applyPath(Long.MAX_VALUE)), Map.of())).andExpect(status().isNotFound());
        call(null, get("/api/jobs/" + Long.MAX_VALUE)).andExpect(status().isNotFound());
    }
    @Test void completeHiringFlowAndRepeatedRequest() throws Exception {
        JobApplication app = application("APPLIED");
        String path = "/api/job-applications/" + app.getId() + "/status";
        for (String next : List.of("SHORTLISTED", "INTERVIEW", "OFFERED", "HIRED", "HIRED")) {
            call(employerUser, patch(path).param("status", next)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value(next));
        }
        call(employerUser, patch(path).param("status", "REJECTED")).andExpect(status().isConflict());
        assertThat(applications.findById(app.getId()).orElseThrow().getStatus()).isEqualTo("HIRED");
    }
    @Test void cannotSkipStagesOrReturnFromRejection() throws Exception {
        JobApplication app = application("APPLIED");
        String path = "/api/job-applications/" + app.getId() + "/status";
        call(employerUser, patch(path).param("status", "HIRED")).andExpect(status().isConflict());
        assertThat(applications.findById(app.getId()).orElseThrow().getStatus()).isEqualTo("APPLIED");
        call(employerUser, patch(path).param("status", "REJECTED")).andExpect(status().isOk());
        call(employerUser, patch(path).param("status", "SHORTLISTED")).andExpect(status().isConflict());
    }
    @Test void invalidOrMissingStatusIs400() throws Exception {
        JobApplication app = application("APPLIED");
        String path = "/api/job-applications/" + app.getId() + "/status";
        call(employerUser, patch(path).param("status", "ACCEPTED")).andExpect(status().isBadRequest());
        call(employerUser, patch(path)).andExpect(status().isBadRequest());
        call(null, get("/api/jobs/status/INVALID")).andExpect(status().isBadRequest());
    }
    @Test void closingJobPreventsNewApplicationsButAllowsExistingReview() throws Exception {
        JobApplication app = application("APPLIED");
        call(employerUser, body(put("/api/jobs/" + job.getId()), Map.of("title", "Closed job", "status", "CLOSED",
                "applicationDeadline", now.minusDays(1).toString()))).andExpect(status().isOk());
        call(candidateUser, body(post(applyPath(job.getId())), Map.of())).andExpect(status().isConflict());
        call(employerUser, patch("/api/job-applications/" + app.getId() + "/status").param("status", "SHORTLISTED"))
                .andExpect(status().isOk());
    }
    @Test void jobDeadlineValidationAndReopening() throws Exception {
        String create = "/api/jobs/employer/" + employer.getId();
        call(employerUser, body(post(create), Map.of("title", "Expired", "applicationDeadline", now.toString())))
                .andExpect(status().isBadRequest());
        call(employerUser, body(post(create), Map.of("title", "Invalid", "status", "INVALID")))
                .andExpect(status().isBadRequest());
        job.setStatus("CLOSED");jobs.saveAndFlush(job);
        String update = "/api/jobs/" + job.getId();
        call(employerUser, body(put(update), Map.of("title", "Expired", "status", "OPEN", "applicationDeadline", now.toString())))
                .andExpect(status().isBadRequest());
        assertThat(jobs.findById(job.getId()).orElseThrow().getStatus()).isEqualTo("CLOSED");
        call(employerUser, body(put(update), Map.of("title", "Reopened", "status", "OPEN", "applicationDeadline", now.plusDays(2).toString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("OPEN"));
    }
    @Test void invalidSalaryAndTitleReturnFieldErrors() throws Exception {
        String path = "/api/jobs/employer/" + employer.getId();
        call(employerUser, body(post(path), Map.of("title", " ", "salaryMin", -1)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.title").exists()).andExpect(jsonPath("$.errors.salaryMin").exists());
        call(employerUser, body(post(path), Map.of("title", "Invalid range", "salaryMin", 100, "salaryMax", 50)))
                .andExpect(status().isBadRequest());
        call(employerUser, body(post(path), Map.of("title", "Valid range", "salaryMin", 50, "salaryMax", 100)))
                .andExpect(status().isCreated());
    }
    @Test void applicationValidationAndMalformedJsonAre400() throws Exception {
        call(candidateUser, body(post(applyPath(job.getId())), Map.of("resumeUrl", "x".repeat(501))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.resumeUrl").exists());
        call(candidateUser, post(applyPath(job.getId())).contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest());
    }
}
