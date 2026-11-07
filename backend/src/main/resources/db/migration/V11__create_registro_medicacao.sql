CREATE TABLE registro_medicacao (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    internacao_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    medicamento VARCHAR(100) NOT NULL,
    dose VARCHAR(50) NOT NULL,
    unidade VARCHAR(30) NOT NULL,
    via VARCHAR(50) NOT NULL,
    prescricao VARCHAR(255) NOT NULL,
    observacoes TEXT,
    data_hora_registro TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_registro_medicacao_internacao
        FOREIGN KEY (internacao_id) REFERENCES internacao(id),
    CONSTRAINT fk_registro_medicacao_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_registro_medicacao_internacao_data
    ON registro_medicacao (internacao_id, data_hora_registro);
