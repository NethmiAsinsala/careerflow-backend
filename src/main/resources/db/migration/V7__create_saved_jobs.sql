CREATE TABLE saved_jobs (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            job_id BIGINT NOT NULL,
                            job_seeker_id BIGINT NOT NULL,
                            saved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            CONSTRAINT fk_saved_jobs_job
                                FOREIGN KEY (job_id)
                                    REFERENCES jobs(id)
                                    ON DELETE CASCADE,

                            CONSTRAINT fk_saved_jobs_job_seeker
                                FOREIGN KEY (job_seeker_id)
                                    REFERENCES job_seekers(id)
                                    ON DELETE CASCADE,

                            CONSTRAINT uk_saved_job
                                UNIQUE (job_id, job_seeker_id)
);