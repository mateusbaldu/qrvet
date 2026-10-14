CREATE TABLE audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    timestamp TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    user_id BIGINT NULL,
    user_email VARCHAR(255) NULL,
    ip_address VARCHAR(45) NULL,
    action VARCHAR(100) NOT NULL,
    resource_name VARCHAR(100) NOT NULL,
    resource_id VARCHAR(100) NULL,
    details JSON NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_audit_logs_timestamp
    ON audit_logs (timestamp);

CREATE INDEX idx_audit_logs_user_id
    ON audit_logs (user_id);

CREATE INDEX idx_audit_logs_action
    ON audit_logs (action);

CREATE INDEX idx_audit_logs_resource
    ON audit_logs (resource_name, resource_id);
