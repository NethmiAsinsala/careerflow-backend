-- ============================================
-- CareerFlow - Job Seeker and Employer Tables
-- ============================================

CREATE TABLE job_seekers (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             user_id BIGINT NOT NULL UNIQUE,

                             CONSTRAINT fk_job_seekers_user
                                 FOREIGN KEY (user_id)
                                     REFERENCES users(id)
);

CREATE TABLE employers (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           user_id BIGINT NOT NULL UNIQUE,

                           CONSTRAINT fk_employers_user
                               FOREIGN KEY (user_id)
                                   REFERENCES users(id)
);