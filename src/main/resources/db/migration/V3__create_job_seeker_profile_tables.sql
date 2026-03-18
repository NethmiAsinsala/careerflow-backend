CREATE TABLE skills (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        job_seeker_id BIGINT NOT NULL,
                        name VARCHAR(100) NOT NULL,
                        CONSTRAINT fk_skills_job_seeker
                            FOREIGN KEY (job_seeker_id)
                                REFERENCES job_seekers(id)
                                ON DELETE CASCADE
);

CREATE TABLE education (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           job_seeker_id BIGINT NOT NULL,
                           institution VARCHAR(255) NOT NULL,
                           degree VARCHAR(255) NOT NULL,
                           field_of_study VARCHAR(255),
                           start_date DATE,
                           end_date DATE,
                           description TEXT,
                           CONSTRAINT fk_education_job_seeker
                               FOREIGN KEY (job_seeker_id)
                                   REFERENCES job_seekers(id)
                                   ON DELETE CASCADE
);

CREATE TABLE experience (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            job_seeker_id BIGINT NOT NULL,
                            company_name VARCHAR(255) NOT NULL,
                            job_title VARCHAR(255) NOT NULL,
                            start_date DATE,
                            end_date DATE,
                            description TEXT,
                            CONSTRAINT fk_experience_job_seeker
                                FOREIGN KEY (job_seeker_id)
                                    REFERENCES job_seekers(id)
                                    ON DELETE CASCADE
);

CREATE TABLE projects (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          job_seeker_id BIGINT NOT NULL,
                          name VARCHAR(255) NOT NULL,
                          description TEXT,
                          technologies TEXT,
                          project_url VARCHAR(500),
                          CONSTRAINT fk_projects_job_seeker
                              FOREIGN KEY (job_seeker_id)
                                  REFERENCES job_seekers(id)
                                  ON DELETE CASCADE
);