# Evoluções, alertas e perfil

Base: `/qrvet/api/v1` no navegador; `/v1` nos controllers. Todos os endpoints abaixo exigem autenticação. Escritas exigem o token CSRF existente.

## Perfil

`PUT /auth/me` recebe `{ "name": "Marina Silva" }` e retorna `UserResponse` atualizado. O nome é obrigatório, tem até 120 caracteres e é salvo sem espaços nas extremidades. Só altera o usuário autenticado; não recebe ID, perfil, e-mail ou senha. A sessão continua válida e `GET /auth/me` retorna o novo nome. A alteração gera auditoria.

## Alertas do paciente

`PUT /pacientes/{id}/alertas` aceita administradores e veterinários. Exemplo:

```json
{
  "alergias": "Alergia conhecida informada pela equipe",
  "restricoesAlimentares": "Evitar ingrediente indicado pelo veterinário",
  "cuidadosManejo": "Sensível a ruídos; manipular com calma"
}
```

Cada campo é opcional e aceita até 1.000 caracteres. A operação substitui os três campos: valores ausentes, nulos ou vazios removem o alerta correspondente. Retorna `PacienteResponse`, incluindo `alertas`. Alterar dados cadastrais ou fotos não remove os alertas. A alteração gera auditoria sem copiar o texto clínico para o log.

Os alertas também aparecem em `InternacaoResponse` e `CuidadoResponse`, permitindo a consulta pelo auxiliar nos cuidados sem liberar dados do tutor. Recepcionistas consultam os alertas na ficha/internação, mas não podem editá-los. O acompanhamento público do tutor não inclui alertas.

## Evoluções da internação

Administradores, veterinários e auxiliares técnicos podem consultar e registrar evoluções. Recepcionistas e tutores não têm acesso a esses endpoints.

`POST /internacoes/{id}/evolucoes` recebe `{ "texto": "Paciente aceitou alimento.\nTutor trouxe ração." }` e retorna HTTP 201 com:

```json
{
  "id": 1,
  "internacaoId": 42,
  "profissionalId": 7,
  "profissionalNome": "Marina Silva",
  "texto": "Paciente aceitou alimento.\nTutor trouxe ração.",
  "dataHoraRegistro": "2026-11-10T15:00:00Z"
}
```

O texto é obrigatório, aceita até 10.000 caracteres e preserva quebras de linha. O servidor define o horário, a autoria e uma cópia do nome do profissional no momento do registro. Mudanças posteriores no perfil não reescrevem a autoria histórica. Só é possível registrar em internações ativas. O bloqueio transacional da internação serializa o registro com o encerramento. A criação gera auditoria com os IDs, sem copiar o texto clínico.

`GET /internacoes/{id}/evolucoes?page=0&size=5` retorna o envelope existente `items`, `page`, `size`, `total`, `pages`. Os registros são ordenados por horário decrescente e ID decrescente para desempate. `page` deve ser não negativo; `size` deve estar entre 1 e 100. A interface apresenta cinco registros por página.

O histórico continua consultável após alta/óbito. Não há edição ou exclusão de evoluções; correções são novas anotações na internação ativa. O acompanhamento público não inclui esse histórico.

## Foto na internação

`InternacaoResponse` e `CuidadoResponse` incluem `fotoVersao` para identificar a versão atual da foto. `GET /internacoes/{id}/foto` retorna a foto atual do paciente como `image/jpeg`, com `Cache-Control: no-store`, para administradores, veterinários, recepcionistas e auxiliares técnicos. Retorna 404 quando a internação/foto não existe. Tutores e acessos anônimos não podem consultar esse endpoint. O acesso do auxiliar ao cadastro completo do paciente continua bloqueado. A interface carrega a imagem com a sessão autenticada, libera os URLs temporários e mantém a identificação textual quando não há foto ou ocorre falha de carregamento.

## Erros e persistência

- 400: conteúdo inválido, limites excedidos, paginação inválida ou tentativa de anotar após encerramento.
- 401: sessão ausente ou inválida.
- 403: perfil sem permissão ou CSRF ausente/inválido.
- 404: paciente/internação inexistente.

A migração Flyway `V12__patient_alerts_and_evolution_notes.sql` adiciona os campos opcionais do paciente e a tabela de evoluções, com chaves estrangeiras e índice para o histórico. Não altera os dados anteriores.

## Verificações

`backend/mvnw.cmd test` executa os testes reais de controllers, serviços, autorização e banco H2. `AuditLogMigrationTests` aplica as migrações no MySQL 8.4 via Testcontainers, exigindo Docker disponível. `ClinicIntegrationTests` cobre persistência, autoria histórica, validação, alta, paginação e permissões dos novos endpoints.

Com o Vite em execução, `npm run test:clinical-records` em `frontend` verifica os fluxos de interface com respostas HTTP simuladas: salvar/recarregar perfil, falhas e nova tentativa, alertas, remoção de alertas, anotações, paginação, perfis e celular. `npm run test:e2e` inclui esse cenário nas regressões existentes.
