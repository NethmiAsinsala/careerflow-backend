CREATE TABLE job_applications (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                  job_id BIGINT NOT NULL,

                                  job_seeker_id BIGINT NOT NULL,

                                  cover_letter TEXT,

                                  resume_url VARCHAR(500),

                                  status VARCHAR(50) NOT NULL DEFAULT 'APPLIED',

                                  applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                                      ON UPDATE CURRENT_TIMESTAMP,

                                  CONSTRAINT fk_job_applications_job
                                      FOREIGN KEY (job_id)
                                          REFERENCES jobs(id)
                                          ON DELETE CASCADE,

                                  CONSTRAINT fk_job_applications_job_seeker
                                      FOREIGN KEY (job_seeker_id)
                                          REFERENCES job_seekers(id)
                                          ON DELETE CASCADE,

                                  CONSTRAINT uk_job_application
                                      UNIQUE (job_id, job_seeker_id)
);