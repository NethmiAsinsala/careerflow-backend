package com.careerflow.security;

import com.careerflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Ownership is resolved through database relationships, never from a submitted user ID. */
@Component("resourceAccess")
@RequiredArgsConstructor
public class ResourceAccess {
    private final JobSeekerRepository jobSeekers;
    private final EmployerRepository employers;
    private final CompanyRepository companies;
    private final JobRepository jobs;
    private final JobApplicationRepository applications;

    private AuthenticatedUser currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AuthenticatedUser user ? user : null;
    }

    public boolean isAdmin() {
        var user = currentUser();
        return user != null && "ADMIN".equals(user.role());
    }

    public boolean ownsJobSeeker(Long id) {
        var user = currentUser();
        return id != null && user != null && "JOB_SEEKER".equals(user.role())
                && jobSeekers.existsByIdAndUser_Id(id, user.id());
    }

    public boolean managesJobSeeker(Long id) {
        return isAdmin() || ownsJobSeeker(id);
    }

    public boolean readsJobSeeker(Long id) {
        if (managesJobSeeker(id)) return true;
        var user = currentUser();
        return id != null && user != null && "EMPLOYER".equals(user.role())
                && applications.existsByJobSeeker_IdAndJob_Employer_User_Id(id, user.id());
    }

    public boolean managesEmployer(Long id) {
        if (isAdmin()) return true;
        var user = currentUser();
        return id != null && user != null && "EMPLOYER".equals(user.role())
                && employers.existsByIdAndUser_Id(id, user.id());
    }

    public boolean readsEmployerUser(Long userId) {
        if (isAdmin()) return true;
        var user = currentUser();
        return user != null && "EMPLOYER".equals(user.role()) && user.id().equals(userId);
    }

    public boolean managesCompany(Long id) {
        if (isAdmin()) return true;
        var user = currentUser();
        return id != null && user != null && "EMPLOYER".equals(user.role())
                && companies.existsByIdAndEmployer_User_Id(id, user.id());
    }

    public boolean managesJob(Long id) {
        if (isAdmin()) return true;
        var user = currentUser();
        return id != null && user != null && "EMPLOYER".equals(user.role())
                && jobs.existsByIdAndEmployer_User_Id(id, user.id());
    }

    public boolean withdrawsApplication(Long id) {
        if (isAdmin()) return true;
        var user = currentUser();
        return id != null && user != null && "JOB_SEEKER".equals(user.role())
                && applications.existsByIdAndJobSeeker_User_Id(id, user.id());
    }

    public boolean reviewsApplication(Long id) {
        if (isAdmin()) return true;
        var user = currentUser();
        return id != null && user != null && "EMPLOYER".equals(user.role())
                && applications.existsByIdAndJob_Employer_User_Id(id, user.id());
    }

    public boolean readsApplication(Long id) {
        return withdrawsApplication(id) || reviewsApplication(id);
    }
}
