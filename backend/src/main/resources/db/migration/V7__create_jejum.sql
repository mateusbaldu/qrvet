CREATE TABLE jejum (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    internacao_id BIGINT NOT NULL,
    data_hora_inicio TIMESTAMP(6) NOT NULL,
    data_hora_fim TIMESTAMP(6) NULL,
    motivo VARCHAR(255) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_jejum_internacao FOREIGN KEY (internacao_id) REFERENCES internacao(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_jejum_internacao_ativo ON jejum (internacao_id, ativo);
