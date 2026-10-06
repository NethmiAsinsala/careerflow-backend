package com.careerflow.service;

import com.careerflow.security.AuthenticatedUser;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@PreAuthorize("denyAll()")
public class DashboardService {
    private final EntityManager entityManager;
    private final Clock clock;

    public record CandidateDashboard(long totalApplications, Map<String, Long> applicationsByStatus, long savedJobs) { }
    public record EmployerDashboard(long totalJobs, long openJobs, long closedJobs, long availableJobs,
                                    long totalApplications, long uniqueApplicants, Map<String, Long> applicationsByStatus) { }
    public record AdminDashboard(long totalUsers, long activeUsers, Map<String, Long> usersByRole,
                                 long totalJobs, long totalApplications, Map<String, Long> applicationsByStatus) { }

    @PreAuthorize("hasRole('JOB_SEEKER')")
    public CandidateDashboard candidate() {
        Long userId = currentId();
        var statuses = applicationCounts("a.jobSeeker.user.id = :userId", userId);
        return new CandidateDashboard(total(statuses), statuses,
                count("select count(s) from SavedJob s where s.jobSeeker.user.id = :userId", userId));
    }

    @PreAuthorize("hasRole('EMPLOYER')")
    public EmployerDashboard employer() {
        Long userId = currentId();
        var statuses = applicationCounts("a.job.employer.user.id = :userId", userId);
        long available = entityManager.createQuery("select count(j) from Job j where j.employer.user.id = :userId "
                + "and j.status = 'OPEN' and (j.applicationDeadline is null or j.applicationDeadline > :now)", Long.class)
                .setParameter("userId", userId).setParameter("now", LocalDateTime.now(clock)).getSingleResult();
        return new EmployerDashboard(
                count("select count(j) from Job j where j.employer.user.id = :userId", userId),
                count("select count(j) from Job j where j.employer.user.id = :userId and j.status = 'OPEN'", userId),
                count("select count(j) from Job j where j.employer.user.id = :userId and j.status = 'CLOSED'", userId),
                available, total(statuses),
                count("select count(distinct a.jobSeeker.id) from JobApplication a where a.job.employer.user.id = :userId", userId), statuses);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public AdminDashboard admin() {
        Map<String, Long> roles = new LinkedHashMap<>();
        for (String role : List.of("JOB_SEEKER", "EMPLOYER", "ADMIN")) roles.put(role, 0L);
        entityManager.createQuery("select u.role.name, count(u) from User u group by u.role.name", Object[].class)
                .getResultList().forEach(row -> roles.put((String) row[0], (Long) row[1]));
        var statuses = applicationCounts(null, null);
        return new AdminDashboard(count("select count(u) from User u", null),
                count("select count(u) from User u where u.active = true", null), roles,
                count("select count(j) from Job j", null), total(statuses), statuses);
    }

    private Map<String, Long> applicationCounts(String predicate, Long userId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String status : List.of("APPLIED", "SHORTLISTED", "INTERVIEW", "OFFERED", "HIRED", "REJECTED")) counts.put(status, 0L);
        var query = entityManager.createQuery("select a.status, count(a) from JobApplication a"
                + (predicate == null ? "" : " where " + predicate) + " group by a.status", Object[].class);
        if (userId != null) query.setParameter("userId", userId);
        query.getResultList().forEach(row -> counts.put((String) row[0], (Long) row[1]));
        return counts;
    }
    private long count(String jpql, Long userId) {
        var query = entityManager.createQuery(jpql, Long.class);
        if (userId != null) query.setParameter("userId", userId);
        return query.getSingleResult();
    }
    private long total(Map<String, Long> counts) { return counts.values().stream().mapToLong(Long::longValue).sum(); }
    private Long currentId() {
        return ((AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).id();
    }
}
