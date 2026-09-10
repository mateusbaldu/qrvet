CREATE TABLE baia (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    identificacao VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DISPONIVEL',
    observacao VARCHAR(255),
    versao BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_baia_identificacao UNIQUE (identificacao),
    CONSTRAINT chk_baia_status CHECK (status IN ('DISPONIVEL', 'OCUPADA', 'MANUTENCAO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
