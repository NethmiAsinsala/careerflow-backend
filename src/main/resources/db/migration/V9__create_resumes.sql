CREATE TABLE resumes (
 job_seeker_id BIGINT NOT NULL PRIMARY KEY,
 content MEDIUMBLOB NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_resume_job_seeker FOREIGN KEY (job_seeker_id) REFERENCES job_seekers(id) ON DELETE CASCADE
);
