CREATE TABLE registro_alimentacao (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    internacao_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    alimento VARCHAR(100) NOT NULL,
    quantidade VARCHAR(50) NOT NULL,
    aceitacao_observacao TEXT,
    data_hora_registro TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_registro_alimentacao_internacao
        FOREIGN KEY (internacao_id) REFERENCES internacao(id),
    CONSTRAINT fk_registro_alimentacao_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_registro_alimentacao_internacao_data
    ON registro_alimentacao (internacao_id, data_hora_registro);
