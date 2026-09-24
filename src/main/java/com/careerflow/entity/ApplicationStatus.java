package com.careerflow.entity;

public enum ApplicationStatus {
    APPLIED, SHORTLISTED, INTERVIEW, OFFERED, HIRED, REJECTED;

    public boolean canTransitionTo(ApplicationStatus next) {
        if (this == next) return true; // Retrying an already completed update is harmless.
        return switch (this) {
            case APPLIED -> next == SHORTLISTED || next == REJECTED;
            case SHORTLISTED -> next == INTERVIEW || next == REJECTED;
            case INTERVIEW -> next == OFFERED || next == REJECTED;
            case OFFERED -> next == HIRED || next == REJECTED;
            case HIRED, REJECTED -> false;
        };
    }
}
