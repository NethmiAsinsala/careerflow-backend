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
class BackendCompletionIntegrationTest {
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


    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Test void logoutRevokesOnlyPresentedToken() throws Exception {
        String token=jwt.issue(candidateUser.getId()),other=jwt.issue(candidateUser.getId());
        assertThat(token).isNotEqualTo(other);
        mvc.perform(post("/api/auth/logout").header("Authorization","Bearer "+token)).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+other)).andExpect(status().isOk());
        call(null,post("/api/auth/logout")).andExpect(status().isUnauthorized());
    }
    private long certificate(User user) throws Exception {
        var response=call(user,body(post("/api/job-seekers/me/certifications"),Map.of("name","Java certificate","issuer","Institute","issuedOn","2024-01-01","expiresOn","2030-01-01")))
            .andExpect(status().isCreated()).andReturn();
        return json.readTree(response.getResponse().getContentAsString()).path("id").asLong();
    }
    @Test void certificatesSupportCrudAndAggregateProfile() throws Exception {
        long id=certificate(candidateUser);
        call(candidateUser,get("/api/job-seekers/me")).andExpect(jsonPath("$.certifications[0].id").value(id));
        call(candidateUser,body(put("/api/job-seekers/me/certifications/"+id),Map.of("name","Updated","issuer","Institute"))).andExpect(status().isOk());
        call(candidateUser,get("/api/job-seekers/me/certifications")).andExpect(jsonPath("$[0].name").value("Updated"));
        call(candidateUser,delete("/api/job-seekers/me/certifications/"+id)).andExpect(status().isNoContent());
        call(candidateUser,get("/api/job-seekers/me/certifications")).andExpect(jsonPath("$.length()").value(0));
    }
    @Test void certificatesEnforceOwnershipAndApplicantReadAccess() throws Exception {
        long id=certificate(candidateUser);String path="/api/job-seekers/"+candidate.getId()+"/certifications";
        call(otherCandidateUser,delete("/api/job-seekers/me/certifications/"+id)).andExpect(status().isForbidden());
        call(otherCandidateUser,body(put("/api/job-seekers/me/certifications/"+id),Map.of("name","Wrong","issuer","Wrong"))).andExpect(status().isForbidden());
        call(employerUser,get(path)).andExpect(status().isOk());
        call(otherEmployerUser,get(path)).andExpect(status().isForbidden());
        call(employerUser,body(post("/api/job-seekers/me/certifications"),Map.of("name","Wrong","issuer","Wrong"))).andExpect(status().isForbidden());
        call(null,get(path)).andExpect(status().isUnauthorized());
    }
    @Test void invalidCertificatesAreRejected() throws Exception {
        call(candidateUser,body(post("/api/job-seekers/me/certifications"),Map.of("name","Java","issuer","Institute","issuedOn","2024-01-01","expiresOn","2020-01-01"))).andExpect(status().isBadRequest());
        call(candidateUser,body(post("/api/job-seekers/me/certifications"),Map.of("name","Java","issuer","Institute","credentialUrl","javascript:alert(1)"))).andExpect(status().isBadRequest());
    }
    @Test void recommendationsRankUnappliedAvailableJobsOnly() throws Exception {
        String name="UniqueSkill"+UUID.randomUUID();Skill skill=new Skill();skill.setName(name);skill.setJobSeeker(candidate);skills.saveAndFlush(skill);
        Job best=jobs.saveAndFlush(Job.builder().employer(employer).title("Best").status("OPEN").skills(name).build());
        Job partial=jobs.saveAndFlush(Job.builder().employer(employer).title("Partial").status("OPEN").skills(name+", OtherSkill").build());
        job.setSkills(name);jobs.saveAndFlush(job); // already applied: must not appear
        jobs.saveAndFlush(Job.builder().employer(employer).title("Closed").status("CLOSED").skills(name).build());
        jobs.saveAndFlush(Job.builder().employer(employer).title("Expired").status("OPEN").skills(name).applicationDeadline(java.time.LocalDateTime.of(2000,1,1,0,0)).build());
        call(candidateUser,get("/api/job-seekers/me/recommendations")).andExpect(status().isOk())
            .andExpect(jsonPath("$.jobs.length()").value(2)).andExpect(jsonPath("$.jobs[0].job.id").value(best.getId()))
            .andExpect(jsonPath("$.jobs[0].matchPercentage").value(100)).andExpect(jsonPath("$.jobs[1].job.id").value(partial.getId()));
        call(candidateUser,get("/api/job-seekers/me/recommendations").param("limit","1")).andExpect(jsonPath("$.jobs.length()").value(1));
        call(candidateUser,get("/api/job-seekers/me/recommendations").param("limit","51")).andExpect(status().isBadRequest());
        call(employerUser,get("/api/job-seekers/me/recommendations")).andExpect(status().isForbidden());
        call(otherCandidateUser,get("/api/job-seekers/me/recommendations")).andExpect(jsonPath("$.jobs").isEmpty());
    }
    private long report() throws Exception {
        var response=call(candidateUser,body(post("/api/reports"),Map.of("targetType","JOB","targetId",job.getId(),"reason","Misleading job description"))).andExpect(status().isCreated()).andReturn();
        return json.readTree(response.getResponse().getContentAsString()).path("id").asLong();
    }
    @Test void reportsHavePrivateListsAndAdminResolution() throws Exception {
        long id=report();
        call(candidateUser,get("/api/reports/me")).andExpect(jsonPath("$.totalElements").value(1));
        call(otherCandidateUser,get("/api/reports/me")).andExpect(jsonPath("$.totalElements").value(0));
        call(candidateUser,get("/api/admin/reports")).andExpect(status().isForbidden());
        call(employerUser,body(patch("/api/admin/reports/"+id),Map.of("status","REVIEWED","resolution","Checked"))).andExpect(status().isForbidden());
        call(admin,body(patch("/api/admin/reports/"+id),Map.of("status","REVIEWED","resolution","Reviewed job content"))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REVIEWED"));
        call(admin,body(patch("/api/admin/reports/"+id),Map.of("status","DISMISSED","resolution","Changed"))).andExpect(status().isConflict());
        call(admin,get("/api/admin/reports")).andExpect(status().isOk());
    }
    @Test void invalidReportsAndAnonymousAccessAreRejected() throws Exception {
        call(candidateUser,body(post("/api/reports"),Map.of("targetType","JOB","targetId",Long.MAX_VALUE,"reason","Report"))).andExpect(status().isNotFound());
        call(candidateUser,body(post("/api/reports"),Map.of("targetType","INVALID","targetId",job.getId(),"reason","Report"))).andExpect(status().isBadRequest());
        call(null,get("/api/reports/me")).andExpect(status().isUnauthorized());
        call(candidateUser,get("/api/reports/me").param("size","101")).andExpect(status().isBadRequest());
    }
    @Test void corsAllowsConfiguredFrontendOnly() throws Exception {
        mvc.perform(options("/api/auth/login").header("Origin","http://localhost:4200").header("Access-Control-Request-Method","POST").header("Access-Control-Request-Headers","content-type"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:4200"));
        mvc.perform(options("/api/auth/login").header("Origin","https://untrusted.example").header("Access-Control-Request-Method","POST")).andExpect(status().isForbidden());
    }
    @Test void bootstrapCreatesAdminWithoutPromotingExistingUsers() throws Exception {
        String email="bootstrap-"+UUID.randomUUID()+"@example.com";
        var bootstrap=new com.careerflow.config.AdminBootstrap(users,roles,passwordEncoder,email,"test-password-long");
        bootstrap.run(null);bootstrap.run(null);
        var created=users.findByEmail(email).orElseThrow();assertThat(created.getRole().getName()).isEqualTo("ADMIN");
        assertThat(passwordEncoder.matches("test-password-long",created.getPassword())).isTrue();
        var collision=new com.careerflow.config.AdminBootstrap(users,roles,passwordEncoder,candidateUser.getEmail(),"test-password-long");
        assertThatThrownBy(()->collision.run(null)).isInstanceOf(IllegalStateException.class);
        assertThat(candidateUser.getRole().getName()).isEqualTo("JOB_SEEKER");
    }

    @Test void legacyProfileValidationAndMissingIdsUseClientErrors() throws Exception {
        String path="/api/job-seekers/"+candidate.getId();
        call(candidateUser,body(post(path+"/skills"),Map.of("name"," "))).andExpect(status().isBadRequest());
        call(candidateUser,body(post(path+"/education"),Map.of("institution","Institute","degree","Diploma","startDate","2024-01-01","endDate","2020-01-01"))).andExpect(status().isBadRequest());
        call(candidateUser,delete(path+"/skills/9223372036854775807")).andExpect(status().isNotFound());
        call(candidateUser,body(post("/api/saved-jobs"),Map.of("jobSeekerId",candidate.getId()))).andExpect(status().isBadRequest());
        call(candidateUser,body(post("/api/saved-jobs"),Map.of("jobSeekerId",candidate.getId(),"jobId",job.getId()))).andExpect(status().isCreated());
        call(candidateUser,body(post("/api/saved-jobs"),Map.of("jobSeekerId",candidate.getId(),"jobId",job.getId()))).andExpect(status().isConflict());
    }
    @Test void adminCannotCreateEmployerForCandidateRole() throws Exception {
        call(admin,body(post("/api/employers"),Map.of("userId",candidateUser.getId()))).andExpect(status().isBadRequest());
    }
}
