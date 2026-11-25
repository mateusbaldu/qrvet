ALTER TABLE paciente
    ADD COLUMN alergias VARCHAR(1000),
    ADD COLUMN restricoes_alimentares VARCHAR(1000),
    ADD COLUMN cuidados_manejo VARCHAR(1000);

CREATE TABLE evolucao (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    internacao_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    profissional_nome VARCHAR(120) NOT NULL,
    texto TEXT NOT NULL,
    data_hora_registro TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_evolucao_internacao FOREIGN KEY (internacao_id) REFERENCES internacao(id),
    CONSTRAINT fk_evolucao_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_evolucao_internacao_data ON evolucao (internacao_id, data_hora_registro, id);
