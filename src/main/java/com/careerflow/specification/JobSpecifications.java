package com.careerflow.specification;

import com.careerflow.dto.request.JobSearchRequest;
import com.careerflow.entity.Job;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Locale;

public final class JobSpecifications {
    private JobSpecifications() { }

    public static Specification<Job> matching(JobSearchRequest request, LocalDateTime now) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (hasText(request.getKeyword())) {
                String pattern = contains(request.getKeyword());
                predicates.add(cb.or(cb.like(cb.lower(root.get("title")), pattern, '!'),
                        cb.like(cb.lower(root.get("description")), pattern, '!')));
            }
            if (hasText(request.getLocation())) predicates.add(cb.like(cb.lower(root.get("location")), contains(request.getLocation()), '!'));
            if (hasText(request.getSkill())) predicates.add(cb.like(cb.lower(root.get("skills")), contains(request.getSkill()), '!'));
            if (hasText(request.getEmploymentType())) predicates.add(cb.equal(cb.lower(root.get("employmentType")), normalized(request.getEmploymentType())));
            if (hasText(request.getExperienceLevel())) predicates.add(cb.equal(cb.lower(root.get("experienceLevel")), normalized(request.getExperienceLevel())));
            if (request.getEmployerId() != null) predicates.add(cb.equal(root.get("employer").get("id"), request.getEmployerId()));
            if (request.getStatus() != null) predicates.add(cb.equal(root.get("status"), request.getStatus()));
            // Salary filters use interval overlap; an unknown required endpoint cannot satisfy that filter.
            if (request.getSalaryMin() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("salaryMax"), request.getSalaryMin()));
            if (request.getSalaryMax() != null) predicates.add(cb.lessThanOrEqualTo(root.get("salaryMin"), request.getSalaryMax()));
            if (request.isAvailableOnly()) {
                predicates.add(cb.equal(root.get("status"), "OPEN"));
                predicates.add(cb.or(cb.isNull(root.get("applicationDeadline")), cb.greaterThan(root.get("applicationDeadline"), now)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static boolean hasText(String value) { return value != null && !value.isBlank(); }
    private static String normalized(String value) { return value.strip().toLowerCase(Locale.ROOT); }
    private static String contains(String value) {
        // User input is literal text, including SQL LIKE wildcard characters.
        return "%" + normalized(value).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}
