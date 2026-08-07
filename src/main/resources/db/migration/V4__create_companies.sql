CREATE TABLE companies (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           employer_id BIGINT NOT NULL UNIQUE,
                           name VARCHAR(255) NOT NULL,
                           description TEXT,
                           industry VARCHAR(255),
                           website VARCHAR(500),
                           location VARCHAR(255),
                           logo_url VARCHAR(500),
                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                               ON UPDATE CURRENT_TIMESTAMP,

                           CONSTRAINT fk_companies_employer
                               FOREIGN KEY (employer_id)
                                   REFERENCES employers(id)
                                   ON DELETE CASCADE
);