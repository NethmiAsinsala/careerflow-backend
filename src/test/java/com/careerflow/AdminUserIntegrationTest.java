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
class AdminUserIntegrationTest {
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


    @Test void listsBoundedPageWithoutPasswords() throws Exception {
        call(admin,get("/api/admin/users").param("size","2")).andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2)).andExpect(jsonPath("$.totalElements").value(users.count()))
            .andExpect(jsonPath("$.content[0].id").value(admin.getId()))
            .andExpect(jsonPath("$.content[0].password").doesNotExist());
        call(admin,get("/api/admin/users").param("page","1").param("size","2"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(1));
    }
    @Test void disablingBlocksPreviouslyIssuedTokenAndReactivationRestoresAccess() throws Exception {
        String token=jwt.issue(candidateUser.getId());
        String path="/api/admin/users/"+candidateUser.getId()+"/status";
        call(admin,body(patch(path),Map.of("active",false))).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
        call(admin,body(patch(path),Map.of("active",true))).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        assertThat(candidateUser.getRole().getName()).isEqualTo("JOB_SEEKER");
    }
    @Test void protectsAdminsAndValidatesTargetAndBody() throws Exception {
        call(admin,body(patch("/api/admin/users/"+admin.getId()+"/status"),Map.of("active",false))).andExpect(status().isConflict());
        User another=account("ADMIN");
        call(admin,body(patch("/api/admin/users/"+another.getId()+"/status"),Map.of("active",false))).andExpect(status().isConflict());
        call(admin,body(patch("/api/admin/users/9223372036854775807/status"),Map.of("active",false))).andExpect(status().isNotFound());
        call(admin,body(patch("/api/admin/users/"+candidateUser.getId()+"/status"),Map.of())).andExpect(status().isBadRequest());
    }
    @Test void restrictsBothEndpointsToAdmins() throws Exception {
        for(User user:List.of(candidateUser,employerUser)) {
            call(user,get("/api/admin/users")).andExpect(status().isForbidden());
            call(user,body(patch("/api/admin/users/"+otherCandidateUser.getId()+"/status"),Map.of("active",false))).andExpect(status().isForbidden());
        }
        call(null,get("/api/admin/users")).andExpect(status().isUnauthorized());
        call(null,body(patch("/api/admin/users/"+candidateUser.getId()+"/status"),Map.of("active",false))).andExpect(status().isUnauthorized());
        assertThat(otherCandidateUser.getActive()).isTrue();
    }
    @Test void rejectsUnboundedPagination() throws Exception {
        for(String size:List.of("0","101","abc")) call(admin,get("/api/admin/users").param("size",size)).andExpect(status().isBadRequest());
        call(admin,get("/api/admin/users").param("page","-1")).andExpect(status().isBadRequest());
    }
}
