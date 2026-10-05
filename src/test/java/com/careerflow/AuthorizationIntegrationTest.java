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
class AuthorizationIntegrationTest {
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
    private Map<String, ?> profileBody(String resource) {
        return switch (resource) {
            case "skills", "projects" -> Map.of("name", "Updated");
            case "education" -> Map.of("institution", "Institute", "degree", "Degree");
            default -> Map.of("companyName", "Company", "jobTitle", "Engineer");
        };
    }
    private Long child(String resource, JobSeeker owner) {
        return switch (resource) {
            case "skills" -> { Skill s = new Skill();s.setJobSeeker(owner);s.setName("Original");yield skills.saveAndFlush(s).getId(); }
            case "education" -> { Education e = new Education();e.setJobSeeker(owner);e.setInstitution("Institute");e.setDegree("Degree");yield education.saveAndFlush(e).getId(); }
            case "experience" -> { Experience e = new Experience();e.setJobSeeker(owner);e.setCompanyName("Company");e.setJobTitle("Engineer");yield experience.saveAndFlush(e).getId(); }
            default -> { Project p = new Project();p.setJobSeeker(owner);p.setName("Original");yield projects.saveAndFlush(p).getId(); }
        };
    }

    @ParameterizedTest @ValueSource(strings = {"skills", "education", "experience", "projects"})
    void profileOwnershipAndNestedIds(String resource) throws Exception {
        Long ownChild = child(resource, candidate), foreignChild = child(resource, otherCandidate);
        String own = "/api/job-seekers/" + candidate.getId() + "/" + resource;
        String foreign = "/api/job-seekers/" + otherCandidate.getId() + "/" + resource;
        call(candidateUser, get(own)).andExpect(status().isOk());
        call(candidateUser, get(foreign)).andExpect(status().isForbidden());
        call(candidateUser, body(post(foreign), profileBody(resource))).andExpect(status().isForbidden());
        call(candidateUser, body(put(foreign + "/" + foreignChild), profileBody(resource))).andExpect(status().isForbidden());
        call(candidateUser, delete(foreign + "/" + foreignChild)).andExpect(status().isForbidden());
        call(candidateUser, body(put(own + "/" + foreignChild), profileBody(resource))).andExpect(status().isForbidden());
        call(candidateUser, delete(own + "/" + foreignChild)).andExpect(status().isForbidden());
        call(employerUser, get(own)).andExpect(status().isOk());
        call(otherEmployerUser, get(own)).andExpect(status().isForbidden());
        call(employerUser, body(post(own), profileBody(resource))).andExpect(status().isForbidden());
        call(admin, get(foreign)).andExpect(status().isOk());
        call(candidateUser, body(post(own), profileBody(resource))).andExpect(status().isCreated());
        call(candidateUser, body(put(own + "/" + ownChild), profileBody(resource))).andExpect(status().isOk());
        call(candidateUser, delete(own + "/" + ownChild)).andExpect(status().isNoContent());
    }
    @Test void jobCreationRequiresOwningEmployer() throws Exception {
        String own = "/api/jobs/employer/" + employer.getId();
        call(candidateUser, body(post(own), Map.of("title", "Forged"))).andExpect(status().isForbidden());
        call(otherEmployerUser, body(post(own), Map.of("title", "Forged"))).andExpect(status().isForbidden());
        call(employerUser, body(post(own), Map.of("title", "New job"))).andExpect(status().isCreated());
    }
    @Test void jobEditsAndDeletesRequireOwnership() throws Exception {
        String path = "/api/jobs/" + job.getId();
        call(otherEmployerUser, body(put(path), Map.of("title", "Forged"))).andExpect(status().isForbidden());
        call(candidateUser, body(put(path), Map.of("title", "Forged"))).andExpect(status().isForbidden());
        call(otherEmployerUser, delete(path)).andExpect(status().isForbidden());
        assertThat(jobs.findById(job.getId()).orElseThrow().getTitle()).isEqualTo("Owner job");
        call(employerUser, body(put(path), Map.of("title", "Updated"))).andExpect(status().isOk());
        // Delete a new job with no applications so the assertion concerns authorization only.
        Job empty = jobs.saveAndFlush(Job.builder().employer(employer).title("Empty").status("OPEN").build());
        call(employerUser, delete("/api/jobs/" + empty.getId())).andExpect(status().isNoContent());
    }
    @Test void companyCreationRequiresOwningEmployer() throws Exception {
        String path = "/api/companies/employer/" + employer.getId();
        call(otherEmployerUser, body(post(path), Map.of("name", "Forged"))).andExpect(status().isForbidden());
        call(candidateUser, body(post(path), Map.of("name", "Forged"))).andExpect(status().isForbidden());
        employer.setCompany(null);
        companies.delete(company);companies.flush();
        call(employerUser, body(post(path), Map.of("name", "New company"))).andExpect(status().isCreated());
    }
    @Test void companyEditsAndDeletesRequireOwnership() throws Exception {
        String path = "/api/companies/" + company.getId();
        call(otherEmployerUser, body(put(path), Map.of("name", "Forged"))).andExpect(status().isForbidden());
        call(candidateUser, delete(path)).andExpect(status().isForbidden());
        call(otherEmployerUser, delete(path)).andExpect(status().isForbidden());
        assertThat(companies.findById(company.getId()).orElseThrow().getName()).isEqualTo("Owner company");
        call(employerUser, body(put(path), Map.of("name", "Updated"))).andExpect(status().isOk());
        call(employerUser, delete(path)).andExpect(status().isNoContent());
    }
    @Test void employerManagementIsRestricted() throws Exception {
        call(employerUser, get("/api/employers/" + employer.getId())).andExpect(status().isOk());
        call(employerUser, get("/api/employers/user/" + employerUser.getId())).andExpect(status().isOk());
        call(employerUser, get("/api/employers/" + otherEmployer.getId())).andExpect(status().isForbidden());
        call(employerUser, get("/api/employers/user/" + otherEmployerUser.getId())).andExpect(status().isForbidden());
        call(employerUser, delete("/api/employers/" + employer.getId())).andExpect(status().isForbidden());
        for (User user : List.of(candidateUser, employerUser)) {
            call(user, get("/api/employers")).andExpect(status().isForbidden());
            call(user, body(post("/api/employers"), Map.of("userId", otherCandidateUser.getId())))
                    .andExpect(status().isForbidden());
        }
        call(admin, get("/api/employers")).andExpect(status().isOk());
    }
    @Test void applicationsCannotBeSubmittedForAnotherCandidate() throws Exception {
        String own = "/api/job-applications/job/" + otherJob.getId() + "/job-seeker/" + candidate.getId();
        call(otherCandidateUser, body(post(own), Map.of())).andExpect(status().isForbidden());
        call(employerUser, body(post(own), Map.of())).andExpect(status().isForbidden());
        call(admin, body(post(own), Map.of())).andExpect(status().isForbidden());
        call(candidateUser, body(post(own), Map.of())).andExpect(status().isCreated());
    }
    @Test void applicationDetailsAreVisibleOnlyToParticipantsOrAdmin() throws Exception {
        String path = "/api/job-applications/" + application.getId();
        for (User user : List.of(candidateUser, employerUser, admin)) call(user, get(path)).andExpect(status().isOk());
        for (User user : List.of(otherCandidateUser, otherEmployerUser)) call(user, get(path)).andExpect(status().isForbidden());
    }
    @Test void applicationListsCannotLeakOtherUsersRecords() throws Exception {
        String jobPath = "/api/job-applications/job/" + job.getId();
        String candidatePath = "/api/job-applications/job-seeker/" + candidate.getId();
        call(employerUser, get(jobPath)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        call(otherEmployerUser, get(jobPath)).andExpect(status().isForbidden());
        call(candidateUser, get(jobPath)).andExpect(status().isForbidden());
        call(candidateUser, get(candidatePath)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        call(otherCandidateUser, get(candidatePath)).andExpect(status().isForbidden());
        // An employer may see this candidate's application to their job, not all of the candidate's applications.
        call(employerUser, get(candidatePath)).andExpect(status().isForbidden());
        for (String path : List.of("/api/job-applications", "/api/job-applications/status/APPLIED")) {
            call(candidateUser, get(path)).andExpect(status().isForbidden());
            call(employerUser, get(path)).andExpect(status().isForbidden());
            call(admin, get(path)).andExpect(status().isOk());
        }
    }
    @Test void onlyOwningEmployerCanReviewApplication() throws Exception {
        String path = "/api/job-applications/" + application.getId() + "/status";
        call(candidateUser, patch(path).param("status", "REJECTED")).andExpect(status().isForbidden());
        call(otherEmployerUser, patch(path).param("status", "REJECTED")).andExpect(status().isForbidden());
        assertThat(applications.findById(application.getId()).orElseThrow().getStatus()).isEqualTo("APPLIED");
        call(employerUser, patch(path).param("status", "SHORTLISTED")).andExpect(status().isOk());
    }
    @Test void onlyOwningCandidateCanWithdrawApplication() throws Exception {
        String path = "/api/job-applications/" + application.getId();
        call(otherCandidateUser, delete(path)).andExpect(status().isForbidden());
        call(employerUser, delete(path)).andExpect(status().isForbidden());
        assertThat(applications.existsById(application.getId())).isTrue();
        call(candidateUser, delete(path)).andExpect(status().isNoContent());
    }
    @Test void savedJobsBelongToCandidateInTokenNotRequestBody() throws Exception {
        var data = Map.of("jobId", job.getId(), "jobSeekerId", candidate.getId());
        call(otherCandidateUser, body(post("/api/saved-jobs"), data)).andExpect(status().isForbidden());
        call(employerUser, body(post("/api/saved-jobs"), data)).andExpect(status().isForbidden());
        call(candidateUser, body(post("/api/saved-jobs"), data)).andExpect(status().isCreated());
        String list = "/api/saved-jobs/job-seeker/" + candidate.getId();
        String remove = "/api/saved-jobs/job/" + job.getId() + "/job-seeker/" + candidate.getId();
        call(otherCandidateUser, get(list)).andExpect(status().isForbidden());
        call(otherCandidateUser, delete(remove)).andExpect(status().isForbidden());
        call(candidateUser, get(list)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        call(candidateUser, delete(remove)).andExpect(status().isNoContent());
    }
    @Test void administratorCanModerateResources() throws Exception {
        call(admin, body(put("/api/jobs/" + job.getId()), Map.of("title", "Moderated"))).andExpect(status().isOk());
        call(admin, body(put("/api/companies/" + company.getId()), Map.of("name", "Moderated"))).andExpect(status().isOk());
        call(admin, patch("/api/job-applications/" + application.getId() + "/status").param("status", "REJECTED"))
                .andExpect(status().isOk());
    }
    @Test void anonymousRequestsAre401AndJobBrowsingStillWorks() throws Exception {
        call(null, get("/api/job-applications/" + application.getId())).andExpect(status().isUnauthorized());
        call(null, body(put("/api/jobs/" + job.getId()), Map.of("title", "Forged"))).andExpect(status().isUnauthorized());
        call(null, get("/api/jobs/" + job.getId())).andExpect(status().isOk());
    }
    @Test void serviceLayerAlsoRejectsCrossOwnerCalls() {
        var principal = new AuthenticatedUser(otherEmployerUser.getId(), otherEmployerUser.getEmail(), "EMPLOYER");
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_EMPLOYER"))));
        JobRequest request = JobRequest.builder().title("Forged").build();
        assertThatThrownBy(() -> jobService.updateJob(job.getId(), request)).isInstanceOf(AccessDeniedException.class);
        assertThat(jobs.findById(job.getId()).orElseThrow().getTitle()).isEqualTo("Owner job");
    }
}
