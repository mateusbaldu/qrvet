CREATE TABLE usuario (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    senha_hash VARCHAR(100) NOT NULL,
    perfil VARCHAR(40) NOT NULL,
    ativo BIT NOT NULL,
    versao_autenticacao BIGINT NOT NULL,
    confirmado BIT NOT NULL DEFAULT 1,
    CONSTRAINT uk_usuario_email UNIQUE (email),

    CONSTRAINT chk_usuario_perfil
    CHECK (perfil IN (
        'ADMIN',
        'VETERINARIO',
        'RECEPCIONISTA',
        'AUXILIAR_TECNICO',
        'TUTOR'
    ))
) ENGINE=InnoDB;