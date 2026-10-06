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
class DashboardIntegrationTest {
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


    @Test void candidateCountsOnlyOwnRecordsAndIncludesZeroStatuses() throws Exception {
        savedJobs.saveAndFlush(SavedJob.builder().job(otherJob).jobSeeker(candidate).build());
        call(candidateUser,get("/api/dashboard/candidate").param("userId",otherCandidateUser.getId().toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalApplications").value(1))
            .andExpect(jsonPath("$.applicationsByStatus.APPLIED").value(1))
            .andExpect(jsonPath("$.applicationsByStatus.HIRED").value(0)).andExpect(jsonPath("$.savedJobs").value(1));
        call(otherCandidateUser,get("/api/dashboard/candidate")).andExpect(jsonPath("$.savedJobs").value(0));
        application.setStatus("INTERVIEW");applications.saveAndFlush(application);
        call(candidateUser,get("/api/dashboard/candidate")).andExpect(jsonPath("$.applicationsByStatus.INTERVIEW").value(1))
            .andExpect(jsonPath("$.applicationsByStatus.APPLIED").value(0));
    }
    @Test void employerCountsDistinctApplicantsAndAvailableJobs() throws Exception {
        Job expired=jobs.saveAndFlush(Job.builder().employer(employer).title("Expired").status("OPEN")
            .applicationDeadline(java.time.LocalDateTime.of(2000,1,1,0,0)).build());
        jobs.saveAndFlush(Job.builder().employer(employer).title("Closed").status("CLOSED").build());
        applications.saveAndFlush(JobApplication.builder().job(expired).jobSeeker(candidate).status("REJECTED").build());
        call(employerUser,get("/api/dashboard/employer")).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalJobs").value(3)).andExpect(jsonPath("$.openJobs").value(2))
            .andExpect(jsonPath("$.closedJobs").value(1)).andExpect(jsonPath("$.availableJobs").value(1))
            .andExpect(jsonPath("$.totalApplications").value(2)).andExpect(jsonPath("$.uniqueApplicants").value(1))
            .andExpect(jsonPath("$.applicationsByStatus.REJECTED").value(1));
        call(otherEmployerUser,get("/api/dashboard/employer")).andExpect(jsonPath("$.totalJobs").value(1))
            .andExpect(jsonPath("$.totalApplications").value(1));
    }
    @Test void adminTotalsMatchDatabaseAndTrackInactiveUsers() throws Exception {
        otherCandidateUser.setActive(false);users.saveAndFlush(otherCandidateUser);
        call(admin,get("/api/dashboard/admin")).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalUsers").value(users.count()))
            .andExpect(jsonPath("$.totalJobs").value(jobs.count()))
            .andExpect(jsonPath("$.totalApplications").value(applications.count()))
            .andExpect(jsonPath("$.activeUsers").value(users.findAll().stream().filter(u->Boolean.TRUE.equals(u.getActive())).count()));
    }
    @Test void emptyAccountsHaveZeroCounts() throws Exception {
        User fresh=account("JOB_SEEKER");seeker(fresh);
        call(fresh,get("/api/dashboard/candidate")).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalApplications").value(0)).andExpect(jsonPath("$.savedJobs").value(0));
        User owner=account("EMPLOYER");employers.saveAndFlush(Employer.builder().user(owner).build());
        call(owner,get("/api/dashboard/employer")).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalJobs").value(0)).andExpect(jsonPath("$.uniqueApplicants").value(0));
    }
    @Test void enforcesRoleForEveryDashboard() throws Exception {
        for(String role:List.of("candidate","employer","admin")) {
            String path="/api/dashboard/"+role;
            call(null,get(path)).andExpect(status().isUnauthorized());
            for(User user:List.of(candidateUser,employerUser,admin)) {
                boolean allowed=role.equals("candidate")?user==candidateUser:role.equals("employer")?user==employerUser:user==admin;
                call(user,get(path)).andExpect(allowed?status().isOk():status().isForbidden());
            }
        }
    }
}
