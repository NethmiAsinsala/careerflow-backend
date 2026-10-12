CREATE TABLE certifications (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 job_seeker_id BIGINT NOT NULL,
 name VARCHAR(200) NOT NULL,
 issuer VARCHAR(200) NOT NULL,
 issued_on DATE NULL,
 expires_on DATE NULL,
 credential_url VARCHAR(500) NULL,
 CONSTRAINT fk_certification_owner FOREIGN KEY(job_seeker_id) REFERENCES job_seekers(id) ON DELETE CASCADE
);
CREATE TABLE content_reports (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 reporter_id BIGINT NOT NULL,
 target_type VARCHAR(20) NOT NULL,
 target_id BIGINT NOT NULL,
 reason VARCHAR(2000) NOT NULL,
 status VARCHAR(20) NOT NULL,
 resolution VARCHAR(2000) NULL,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_report_user FOREIGN KEY(reporter_id) REFERENCES users(id) ON DELETE CASCADE,
 INDEX idx_report_status (status), INDEX idx_report_owner (reporter_id)
);
CREATE TABLE revoked_tokens (
 token_hash VARCHAR(64) NOT NULL PRIMARY KEY,
 expires_at DATETIME(6) NOT NULL,
 INDEX idx_revoked_expiry (expires_at)
);
