package com.careerflow;

import com.careerflow.entity.*;
import com.careerflow.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(JobSearchIntegrationTest.FixedTime.class)
@EnabledIfEnvironmentVariable(named = "CAREERFLOW_DB_TESTS", matches = "true")
class JobSearchIntegrationTest {
    @TestConfiguration static class FixedTime {
        @Bean @Primary Clock searchClock() { return Clock.fixed(Instant.parse("2030-01-01T12:00:00Z"), ZoneOffset.UTC); }
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        String secret = Base64.getEncoder().encodeToString(io.jsonwebtoken.Jwts.SIG.HS256.key().build().getEncoded());
        registry.add("app.jwt.secret", () -> secret);
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired EmployerRepository employers;
    @Autowired JobRepository jobs;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    Employer employer;
    Job javaJob, frontend, data, literal;
    final LocalDateTime now = LocalDateTime.of(2030, 1, 1, 12, 0);

    private Job job(String title, String location, String type, String level, String skill, String status,
                    Integer salaryMin, Integer salaryMax, LocalDateTime deadline) {
        Job job = jobs.saveAndFlush(Job.builder().employer(employer).title(title).location(location)
                .employmentType(type).experienceLevel(level).skills(skill).status(status)
                .salaryMin(salaryMin == null ? null : BigDecimal.valueOf(salaryMin))
                .salaryMax(salaryMax == null ? null : BigDecimal.valueOf(salaryMax))
                .applicationDeadline(deadline).build());
        job.setCreatedAt(now.minusDays(1));return jobs.saveAndFlush(job);
    }
    @BeforeEach void setup() {
        User user = new User();user.setEmail("search-test-" + UUID.randomUUID() + "@example.com");
        user.setPassword("unused-test-hash");user.setRole(roles.findByName("EMPLOYER").orElseThrow());users.saveAndFlush(user);
        employer = employers.saveAndFlush(Employer.builder().user(user).build());
        javaJob = job("Java Backend", "Colombo", "FULL_TIME", "JUNIOR", "Java, Spring Boot", "OPEN", 100, 200, now.plusDays(1));
        javaJob.setDescription("Database transactions");jobs.saveAndFlush(javaJob);
        frontend = job("Frontend", "Kandy", "CONTRACT", "SENIOR", "TypeScript", "CLOSED", 300, 400, null);
        data = job("Data Engineer", "Colombo", "FULL_TIME", "SENIOR", "Python", "OPEN", 150, 250, now);
        literal = job("100%_! Engineer", "Remote", "PART_TIME", "JUNIOR", "SQL", "OPEN", null, null, null);
        jdbc.update("UPDATE jobs SET created_at = ? WHERE employer_id = ?", java.sql.Timestamp.valueOf(now.minusDays(1)), employer.getId());
    }
    private ResultActions search(String... pairs) throws Exception {
        var values = new LinkedHashMap<String, String>();
        values.put("employerId", employer.getId().toString());
        for (int i = 0; i < pairs.length; i += 2) values.put(pairs[i], pairs[i + 1]);
        var request = get("/api/jobs");
        values.forEach((key, value) -> request.param(key, value));
        return mvc.perform(request);
    }
    @Test void paginatesWithStableTieBreakAndTotals() throws Exception {
        search("size", "2").andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false)).andExpect(jsonPath("$.content[0].id").value(literal.getId()))
                .andExpect(jsonPath("$.content[1].id").value(data.getId()));
        search("size", "2", "page", "1").andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(frontend.getId()))
                .andExpect(jsonPath("$.content[1].id").value(javaJob.getId())).andExpect(jsonPath("$.last").value(true));
    }
    @Test void searchesTitleAndDescriptionIgnoringCase() throws Exception {
        for (String keyword : List.of(" java ", "TRANSACTIONS"))
            search("keyword", keyword).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(javaJob.getId()));
    }
    @Test void combinesFiltersWithAnd() throws Exception {
        search("location", "col", "skill", "spring boot", "employmentType", "full_time",
                "experienceLevel", "junior", "status", "OPEN")
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(javaJob.getId()));
    }
    @Test void filtersSalaryByOverlap() throws Exception {
        search("salaryMin", "220", "salaryMax", "310", "sort", "salaryMin,asc")
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(data.getId()))
                .andExpect(jsonPath("$.content[1].id").value(frontend.getId()));
    }
    @Test void availableOnlyExcludesClosedAndExactDeadline() throws Exception {
        search("availableOnly", "true").andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        search("availableOnly", "true", "status", "CLOSED").andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }
    @Test void wildcardsAreLiteral() throws Exception {
        search("keyword", "%_!").andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(literal.getId()));
        search("keyword", "' OR 1=1 --").andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }
    @Test void emptyAndOutOfRangePagesAreValid() throws Exception {
        search("keyword", "no-such-result").andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalPages").value(0));
        search("page", "10", "size", "2").andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(4));
    }
    @Test void sortByTitleAndId() throws Exception {
        search("sort", "title,asc").andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(literal.getId()));
        search("sort", "id,asc").andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(javaJob.getId()));
    }
    @Test void defaultsAreBoundedAndPublic() throws Exception {
        mvc.perform(get("/api/jobs")).andExpect(status().isOk()).andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20)).andExpect(jsonPath("$.content").isArray());
    }
    @ParameterizedTest
    @CsvSource({"page,-1", "page,1000001", "size,0", "size,101", "size,abc", "salaryMin,-1",
                "status,INVALID", "employerId,0", "sort,unknown", "sort,'password,asc'", "sort,'title,sideways'"})
    void rejectsInvalidQueryValues(String parameter, String value) throws Exception {
        search(parameter, value).andExpect(status().isBadRequest());
    }
    @Test void rejectsReversedSalaryRange() throws Exception {
        search("salaryMin", "200", "salaryMax", "100").andExpect(status().isBadRequest());
    }
    @Test void documentsIndividualQueryParameters() throws Exception {
        String response = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var parameters = json.readTree(response).path("paths").path("/api/jobs").path("get").path("parameters");
        List<String> names = new ArrayList<>();parameters.forEach(p -> names.add(p.path("name").asText()));
        assertThat(names).contains("keyword", "location", "skill", "page", "size", "sort", "availableOnly");
    }
}
