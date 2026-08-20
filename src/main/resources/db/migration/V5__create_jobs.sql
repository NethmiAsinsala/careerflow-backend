CREATE TABLE jobs (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,

                      employer_id BIGINT NOT NULL,

                      title VARCHAR(255) NOT NULL,

                      description TEXT,

                      location VARCHAR(255),

                      employment_type VARCHAR(100),

                      experience_level VARCHAR(100),

                      salary_min DECIMAL(12,2),

                      salary_max DECIMAL(12,2),

                      skills TEXT,

                      application_deadline TIMESTAMP NULL,

                      status VARCHAR(50) NOT NULL DEFAULT 'OPEN',

                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                          ON UPDATE CURRENT_TIMESTAMP,

                      CONSTRAINT fk_jobs_employer
                          FOREIGN KEY (employer_id)
                              REFERENCES employers(id)
                              ON DELETE CASCADE
);