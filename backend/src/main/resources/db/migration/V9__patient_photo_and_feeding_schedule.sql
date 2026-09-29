ALTER TABLE paciente ADD COLUMN foto_arquivo VARCHAR(80) NULL;

CREATE TABLE alimentacao_agendada (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    internacao_id BIGINT NOT NULL,
    alimento VARCHAR(100) NOT NULL,
    quantidade VARCHAR(50) NOT NULL,
    horario TIMESTAMP(6) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    registro_id BIGINT NULL,
    motivo_cancelamento VARCHAR(255) NULL,
    criado_por BIGINT NOT NULL,
    CONSTRAINT fk_agenda_internacao FOREIGN KEY (internacao_id) REFERENCES internacao(id),
    CONSTRAINT fk_agenda_registro FOREIGN KEY (registro_id) REFERENCES registro_alimentacao(id),
    CONSTRAINT fk_agenda_usuario FOREIGN KEY (criado_por) REFERENCES usuario(id),
    INDEX idx_agenda_internacao_horario (internacao_id, horario),
    INDEX idx_agenda_horario (horario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
