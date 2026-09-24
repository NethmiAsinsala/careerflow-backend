package com.careerflow.service;

import com.careerflow.dto.request.JobRequest;
import com.careerflow.entity.Job;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

class JobWorkflowRulesTest {
    private final LocalDateTime now = LocalDateTime.of(2030, 1, 1, 12, 0);
    private final JobWorkflowRules rules = new JobWorkflowRules(
            Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

    @ParameterizedTest
    @CsvSource({"APPLIED,SHORTLISTED", "APPLIED,REJECTED", "SHORTLISTED,INTERVIEW",
            "SHORTLISTED,REJECTED", "INTERVIEW,OFFERED", "INTERVIEW,REJECTED", "OFFERED,HIRED", "OFFERED,REJECTED"})
    void permitsForwardProgressAndRejection(String current, String next) {
        assertThat(rules.nextApplicationStatus(current, next)).isEqualTo(next);
    }
    @ParameterizedTest
    @CsvSource({"APPLIED,HIRED", "APPLIED,INTERVIEW", "SHORTLISTED,APPLIED", "INTERVIEW,SHORTLISTED",
            "OFFERED,INTERVIEW", "HIRED,REJECTED", "HIRED,APPLIED", "REJECTED,SHORTLISTED", "REJECTED,HIRED"})
    void rejectsSkippingBackwardAndTerminalChanges(String current, String next) {
        assertThatThrownBy(() -> rules.nextApplicationStatus(current, next))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
    }
    @ParameterizedTest @ValueSource(strings = {"APPLIED", "SHORTLISTED", "INTERVIEW", "OFFERED", "HIRED", "REJECTED"})
    void repeatedStatusIsIdempotent(String status) {
        assertThat(rules.nextApplicationStatus(status, status)).isEqualTo(status);
    }
    @ParameterizedTest @ValueSource(strings = {"", "accepted", "ACCEPTED", "WITHDRAWN"})
    void unknownRequestedStatusIs400(String status) {
        assertThatThrownBy(() -> rules.nextApplicationStatus("APPLIED", status))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
    }
    @Test void legacyStatusIsNotSilentlyOverwritten() {
        assertThatThrownBy(() -> rules.nextApplicationStatus("LEGACY", "SHORTLISTED"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
    }
    @Test void deadlineExactlyNowIsClosed() {
        Job job = Job.builder().status("OPEN").applicationDeadline(now).build();
        assertThatThrownBy(() -> rules.requireAcceptingApplications(job))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
    }
    @Test void futureOrAbsentDeadlineIsAllowed() {
        assertThatCode(() -> rules.requireAcceptingApplications(Job.builder().status("OPEN").build())).doesNotThrowAnyException();
        assertThatCode(() -> rules.requireAcceptingApplications(Job.builder().status("OPEN").applicationDeadline(now.plusSeconds(1)).build()))
                .doesNotThrowAnyException();
    }
    @Test void pastDeadlineIsClosed() {
        assertThatThrownBy(() -> rules.requireAcceptingApplications(Job.builder().status("OPEN").applicationDeadline(now.minusSeconds(1)).build()))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
    }
    @Test void closingExpiredJobIsAllowedButOpeningItIsNot() {
        JobRequest request = JobRequest.builder().status("CLOSED").applicationDeadline(now.minusDays(1)).build();
        assertThat(rules.validateJob(request, "OPEN")).isEqualTo("CLOSED");
        request.setStatus("OPEN");
        assertThatThrownBy(() -> rules.validateJob(request, "CLOSED"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
    }
}
