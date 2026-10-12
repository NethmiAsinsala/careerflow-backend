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
class ResumeAnalysisIntegrationTest {
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


    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.careerflow.ai.OllamaResumeClient ai;
    private byte[] pdf() throws Exception {
        try(var doc=new org.apache.pdfbox.pdmodel.PDDocument();var out=new java.io.ByteArrayOutputStream()) {
            var page=new org.apache.pdfbox.pdmodel.PDPage();doc.addPage(page);
            try(var stream=new org.apache.pdfbox.pdmodel.PDPageContentStream(doc,page)) {
                stream.beginText();stream.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA),12);
                stream.newLineAtOffset(40,700);stream.showText("Java Spring Boot developer");stream.endText();
            }
            doc.save(out);return out.toByteArray();
        }
    }
    private void upload(byte[] bytes) throws Exception {
        call(candidateUser,multipart("/api/job-seekers/me/resume").file(new org.springframework.mock.web.MockMultipartFile("file","resume.pdf","application/pdf",bytes))).andExpect(status().isOk());
    }
    @Test void analyzesOwnResumeWithoutChangingProfile() throws Exception {
        byte[] bytes=pdf();upload(bytes);
        org.mockito.Mockito.when(ai.model()).thenReturn("local-test-model");
        org.mockito.Mockito.when(ai.analyze(org.mockito.ArgumentMatchers.anyString())).thenReturn(new com.careerflow.ai.ResumeFeedback("Developer",List.of("Java"),List.of(),List.of("Add project outcomes")));
        call(candidateUser,post("/api/ai/resume/analyze").param("jobSeekerId",otherCandidate.getId().toString()))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.provider").value("ollama")).andExpect(jsonPath("$.feedback.skills[0]").value("Java"))
            .andExpect(jsonPath("$.pages").value(1)).andExpect(jsonPath("$.resumeSha256").value(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes))));
        assertThat(skills.findByJobSeekerIdOrderByIdAsc(candidate.getId())).isEmpty();
        org.mockito.Mockito.verify(ai).analyze(org.mockito.ArgumentMatchers.contains("Java Spring Boot developer"));
    }
    @Test void requiresOwnerResumeAndCandidateRole() throws Exception {
        upload(pdf());
        call(otherCandidateUser,post("/api/ai/resume/analyze")).andExpect(status().isNotFound());
        call(employerUser,post("/api/ai/resume/analyze")).andExpect(status().isForbidden());
        call(admin,post("/api/ai/resume/analyze")).andExpect(status().isForbidden());
        call(null,post("/api/ai/resume/analyze")).andExpect(status().isUnauthorized());
        org.mockito.Mockito.verifyNoInteractions(ai);
    }
    @Test void invalidPdfNeverReachesModel() throws Exception {
        upload("%PDF-fake\n%%EOF".getBytes());
        call(candidateUser,post("/api/ai/resume/analyze")).andExpect(status().isUnprocessableEntity());
        org.mockito.Mockito.verifyNoInteractions(ai);
    }
    @Test void providerFailureReleasesCapacity() throws Exception {
        upload(pdf());
        org.mockito.Mockito.when(ai.analyze(org.mockito.ArgumentMatchers.anyString())).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Ollama unavailable"));
        call(candidateUser,post("/api/ai/resume/analyze")).andExpect(status().isServiceUnavailable());
        call(candidateUser,post("/api/ai/resume/analyze")).andExpect(status().isServiceUnavailable());
        org.mockito.Mockito.verify(ai,org.mockito.Mockito.times(2)).analyze(org.mockito.ArgumentMatchers.anyString());
    }
}

