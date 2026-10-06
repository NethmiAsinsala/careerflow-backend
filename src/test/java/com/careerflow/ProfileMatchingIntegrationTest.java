package com.careerflow;

import com.careerflow.entity.*;
import com.careerflow.repository.*;
import com.careerflow.security.JwtService;
import com.careerflow.security.AuthenticatedUser;
import com.careerflow.service.JobService;
import com.careerflow.dto.request.JobRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "CAREERFLOW_DB_TESTS", matches = "true")
class ProfileMatchingIntegrationTest {
    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        String secret = Base64.getEncoder().encodeToString(io.jsonwebtoken.Jwts.SIG.HS256.key().build().getEncoded());
        registry.add("app.jwt.secret", () -> secret);
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtService jwt;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired JobSeekerRepository seekers;
    @Autowired EmployerRepository employers;
    @Autowired CompanyRepository companies;
    @Autowired JobRepository jobs;
    @Autowired JobApplicationRepository applications;
    @Autowired SavedJobRepository savedJobs;
    @Autowired SkillRepository skills;
    @Autowired EducationRepository education;
    @Autowired ExperienceRepository experience;
    @Autowired ProjectRepository projects;
    @Autowired JobService jobService;

    User candidateUser, otherCandidateUser, employerUser, otherEmployerUser, admin;
    JobSeeker candidate, otherCandidate;
    Employer employer, otherEmployer;
    Job job, otherJob;
    Company company, otherCompany;
    JobApplication application, otherApplication;

    private User account(String role) {
        User user = new User();
        user.setEmail("access-test-" + UUID.randomUUID() + "@example.com");
        user.setPassword("unused-test-hash");
        user.setRole(roles.findByName(role).orElseThrow());
        return users.saveAndFlush(user);
    }
    private JobSeeker seeker(User user) {
        JobSeeker result = new JobSeeker();result.setUser(user);return seekers.saveAndFlush(result);
    }
    @BeforeEach void setup() {
        candidateUser = account("JOB_SEEKER"); otherCandidateUser = account("JOB_SEEKER");
        employerUser = account("EMPLOYER"); otherEmployerUser = account("EMPLOYER"); admin = account("ADMIN");
        candidate = seeker(candidateUser); otherCandidate = seeker(otherCandidateUser);
        employer = employers.saveAndFlush(Employer.builder().user(employerUser).build());
        otherEmployer = employers.saveAndFlush(Employer.builder().user(otherEmployerUser).build());
        company = companies.saveAndFlush(Company.builder().employer(employer).name("Owner company").build());
        otherCompany = companies.saveAndFlush(Company.builder().employer(otherEmployer).name("Other company").build());
        employer.setCompany(company);
        otherEmployer.setCompany(otherCompany);
        job = jobs.saveAndFlush(Job.builder().employer(employer).title("Owner job").status("OPEN").build());
        otherJob = jobs.saveAndFlush(Job.builder().employer(otherEmployer).title("Other job").status("OPEN").build());
        application = applications.saveAndFlush(JobApplication.builder().job(job).jobSeeker(candidate).status("APPLIED").build());
        otherApplication = applications.saveAndFlush(JobApplication.builder().job(otherJob).jobSeeker(otherCandidate).status("APPLIED").build());
    }
    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }
    private ResultActions call(User user, MockHttpServletRequestBuilder request) throws Exception {
        if (user != null) request.header("Authorization", "Bearer " + jwt.issue(user.getId()));
        return mvc.perform(request);
    }
    private MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder request, Map<String, ?> body) throws Exception {
        return request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
    }

    @Test void updatesOwnProfileAndPreservesIdentity() throws Exception {
        call(candidateUser, body(put("/api/job-seekers/me"), Map.of("firstName", " Ada ", "lastName", "Lovelace", "headline", "Developer", "userId", otherCandidateUser.getId(), "email", "wrong@example.com")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.firstName").value("Ada"))
            .andExpect(jsonPath("$.userId").value(candidateUser.getId())).andExpect(jsonPath("$.email").value(candidateUser.getEmail()));
        call(candidateUser, get("/api/job-seekers/me")).andExpect(status().isOk()).andExpect(jsonPath("$.headline").value("Developer"));
        call(candidateUser, body(put("/api/job-seekers/me"), Map.of("firstName", "Ada", "lastName", "Lovelace")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.headline").doesNotExist());
        assertThat(otherCandidate.getFirstName()).isNull();
    }
    @Test void profileAccessIsRestricted() throws Exception {
        String path = "/api/job-seekers/" + candidate.getId();
        call(candidateUser,get(path)).andExpect(status().isOk());
        call(otherCandidateUser,get(path)).andExpect(status().isForbidden());
        call(employerUser,get(path)).andExpect(status().isOk());
        call(otherEmployerUser,get(path)).andExpect(status().isForbidden());
        call(admin,get(path)).andExpect(status().isOk());
        call(null,get(path)).andExpect(status().isUnauthorized());
        call(employerUser,get("/api/job-seekers/me")).andExpect(status().isForbidden());
        call(employerUser,body(put("/api/job-seekers/me"),Map.of("firstName","A","lastName","B"))).andExpect(status().isForbidden());
    }
    @ParameterizedTest @ValueSource(strings={"firstName", "phone", "githubUrl", "summary"})
    void invalidProfileIsRejected(String field) throws Exception {
        Map<String,Object> data=new HashMap<>(Map.of("firstName","Ada","lastName","Lovelace"));
        data.put(field, switch(field) {case "firstName" -> " "; case "phone" -> "abc"; case "githubUrl" -> "javascript:alert(1)"; default -> "x".repeat(3001);});
        call(candidateUser,body(put("/api/job-seekers/me"),data)).andExpect(status().isBadRequest());
    }
    private void skill(JobSeeker owner,String name) { Skill s=new Skill();s.setJobSeeker(owner);s.setName(name);skills.saveAndFlush(s); }
    @Test void aggregatesOnlyOwnSkills() throws Exception {
        skill(candidate,"Java");skill(otherCandidate,"Python");
        call(candidateUser,get("/api/job-seekers/me")).andExpect(status().isOk())
            .andExpect(jsonPath("$.skills.length()").value(1)).andExpect(jsonPath("$.skills[0].name").value("Java"))
            .andExpect(jsonPath("$.password").doesNotExist());
    }
    @Test void matchesAuthenticatedCandidatesSkills() throws Exception {
        job.setSkills("Java, Spring Boot;SQL\nJava");jobs.saveAndFlush(job);
        skill(candidate," java ");skill(candidate,"Spring   Boot");skill(otherCandidate,"SQL");
        String path="/api/jobs/"+job.getId()+"/match";
        call(candidateUser,get(path).param("jobSeekerId",otherCandidate.getId().toString())).andExpect(status().isOk())
            .andExpect(jsonPath("$.matchPercentage").value(67)).andExpect(jsonPath("$.requiredSkillCount").value(3))
            .andExpect(jsonPath("$.missingSkills[0]").value("SQL")).andExpect(jsonPath("$.jobSeekerId").value(candidate.getId()));
        call(otherCandidateUser,get(path)).andExpect(status().isOk()).andExpect(jsonPath("$.matchPercentage").value(33));
    }
    @Test void matchRequiresCandidateAndExistingJob() throws Exception {
        String path="/api/jobs/"+job.getId()+"/match";
        call(null,get(path)).andExpect(status().isUnauthorized());
        call(employerUser,get(path)).andExpect(status().isForbidden());
        call(admin,get(path)).andExpect(status().isForbidden());
        call(candidateUser,get("/api/jobs/9223372036854775807/match")).andExpect(status().isNotFound());
        call(candidateUser,get(path)).andExpect(status().isOk()).andExpect(jsonPath("$.matchPercentage").doesNotExist());
    }
}
