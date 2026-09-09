CREATE TABLE internacao (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    paciente_id BIGINT NOT NULL,
    baia_id BIGINT NOT NULL,
    veterinario_id BIGINT NOT NULL,
    uuid_token VARCHAR(36) NOT NULL,
    entrada_internacao TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    saida_internacao TIMESTAMP(6) NULL,
    motivo TEXT NOT NULL,
    diagnostico_inicial VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ATIVA',
    observacoes TEXT,
    versao BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_internacao_uuid_token UNIQUE (uuid_token),
    CONSTRAINT fk_internacao_paciente FOREIGN KEY (paciente_id) REFERENCES paciente(id),
    CONSTRAINT fk_internacao_baia FOREIGN KEY (baia_id) REFERENCES baia(id),
    CONSTRAINT fk_internacao_veterinario FOREIGN KEY (veterinario_id) REFERENCES usuario(id),
    CONSTRAINT chk_internacao_status CHECK (status IN ('ATIVA', 'ALTA', 'OBITO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_internacao_status ON internacao (status);
CREATE INDEX idx_internacao_paciente_status ON internacao (paciente_id, status);
